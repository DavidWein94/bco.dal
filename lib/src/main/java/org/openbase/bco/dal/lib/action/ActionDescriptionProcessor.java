package org.openbase.bco.dal.lib.action;

import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.annotation.Experimental;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription.Builder;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescriptionOrBuilder;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator.InitiatorType;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameterOrBuilder;
import org.openbase.type.domotic.action.ActionPriorityType.ActionPriority.Priority;
import org.openbase.type.domotic.action.ActionReferenceType.ActionReference;
import org.openbase.type.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.language.MultiLanguageTextType.MultiLanguageText;
import org.openbase.type.language.MultiLanguageTextType.MultiLanguageText.MapFieldEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ActionDescriptionProcessor {

    public static final String INITIATOR_KEY = "$INITIATOR";
    public static final String SERVICE_TYPE_KEY = "$SERVICE_TYPE";
    public static final String UNIT_LABEL_KEY = "$UNIT_LABEL";
    public static final String SERVICE_ATTRIBUTE_KEY = "SERVICE_ATTRIBUTE";
    public static final String GENERIC_ACTION_LABEL = UNIT_LABEL_KEY + "[" + SERVICE_ATTRIBUTE_KEY + "]";

    public static final Map<String, String> GENERIC_ACTION_DESCRIPTION_MAP = new HashMap<>();

    static {
        GENERIC_ACTION_DESCRIPTION_MAP.put("en", INITIATOR_KEY + " changed " + SERVICE_TYPE_KEY + " of " + UNIT_LABEL_KEY + " to " + SERVICE_ATTRIBUTE_KEY + ".");
        GENERIC_ACTION_DESCRIPTION_MAP.put("de", INITIATOR_KEY + " hat " + SERVICE_TYPE_KEY + "  von " + UNIT_LABEL_KEY + " zu " + SERVICE_ATTRIBUTE_KEY + " geändert.");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionDescriptionProcessor.class);
    private static final ServiceJSonProcessor JSON_PROCESSOR = new ServiceJSonProcessor();

    public static ActionParameter.Builder generateDefaultActionParameter(final Message serviceAttribute, final ServiceType serviceType, final UnitType unitType) throws CouldNotPerformException {
        return generateDefaultActionParameter(generateServiceStateDescription(serviceAttribute, serviceType).setUnitType(unitType).build(), true);
    }

    public static ActionParameter.Builder generateDefaultActionParameter(final Message serviceAttribute, final ServiceType serviceType, final Unit<?> unit) throws CouldNotPerformException {
        return generateDefaultActionParameter(generateServiceStateDescription(serviceAttribute, serviceType, unit), true);
    }

    public static ActionParameter.Builder generateDefaultActionParameter(final Message serviceAttribute, final ServiceType serviceType, final Unit<?> unit, final boolean authenticated) throws CouldNotPerformException {
        return generateDefaultActionParameter(generateServiceStateDescription(serviceAttribute, serviceType, unit), authenticated);
    }

    public static ActionParameter.Builder generateDefaultActionParameter(final Message serviceAttribute, final ServiceType serviceType) throws CouldNotPerformException {
        return generateDefaultActionParameter(generateServiceStateDescription(serviceAttribute, serviceType).build(), true);
    }


    public static ActionParameter.Builder generateDefaultActionParameter(final ServiceStateDescription serviceStateDescription) {
        return generateDefaultActionParameter(serviceStateDescription, true);
    }

    /**
     * Generates a message of default {@code ActionParameter}.
     * <p>
     * These are:
     * <ul>
     * <li>Priority = NORMAL</li>
     * <li>ExecutionTimePeriod = 0</li>
     * </ul>
     *
     * @param serviceStateDescription the description of which service how to manipulate.
     *
     * @return an ActionParameter type with the described values.
     */
    public static ActionParameter.Builder generateDefaultActionParameter(final ServiceStateDescription serviceStateDescription, final boolean authenticated) {
        ActionParameter.Builder actionParameter = ActionParameter.newBuilder();
        actionParameter.setServiceStateDescription(serviceStateDescription);
        actionParameter.setPriority(Priority.NORMAL);
        actionParameter.setExecutionTimePeriod(0);
        actionParameter.setActionInitiator(detectActionInitiator(authenticated));
        return actionParameter;
    }

    /**
     * Build an ActionReference from a given ActionDescription which can be added to an action chain.
     *
     * @param actionDescription the ActionDescription from which the ActionReference is generated.
     *
     * @return an ActionReference for the given ActionDescription.
     */
    public static ActionReference generateActionReference(final ActionDescriptionOrBuilder actionDescription) {
        ActionReference.Builder actionReference = ActionReference.newBuilder();
        actionReference.setActionId(actionDescription.getId());
        actionReference.setActionInitiator(actionDescription.getActionInitiator());
        actionReference.setServiceStateDescription(actionDescription.getServiceStateDescription());
        return actionReference.build();
    }

    /**
     * Build an ActionReference from a given ActionParameter which can be added to an action chain.
     *
     * @param actionParameter the ActionParameter from which the ActionReference is generated.
     *
     * @return an ActionReference for the given ActionParameter.
     */
    public static ActionReference generateActionReference(final ActionParameterOrBuilder actionParameter) {
        ActionReference.Builder actionReference = ActionReference.newBuilder();
        actionReference.setActionId(actionParameter.getActionInitiator().getInitiatorId());
        actionReference.setActionInitiator(actionParameter.getActionInitiator());
        actionReference.setServiceStateDescription(actionParameter.getServiceStateDescription());
        return actionReference.build();
    }

    /**
     * Updates the ActionChain which is a description of actions that lead to this action.
     * The action chain is updated in a way that the immediate parent is the first element of
     * the chain. The index of the chain indicates how many actions are in between this
     * action and the causing action.
     *
     * @param actionDescription the ActionDescription which is updated
     * @param parentAction      the ActionDescription of the action which is the cause for the new action
     *
     * @return the updated ActionDescription
     */
    public static ActionDescription.Builder updateActionCause(final ActionDescription.Builder actionDescription, final ActionDescriptionOrBuilder parentAction) {
        actionDescription.clearActionCause();
        actionDescription.addActionCause(generateActionReference(parentAction));
        actionDescription.addAllActionCause(parentAction.getActionCauseList());
        return actionDescription;
    }

    /**
     * Return the initial initiator of an action. According to {@link #updateActionCause(Builder, ActionDescriptionOrBuilder)}
     * the immediate parent of an action is the first element in its chain. Thus, the last element of the chain contains
     * the original initiator. If the action chain is empty, the initiator of the action is returned.
     *
     * @param actionDescription the action description from which the original initiator is resolved.
     *
     * @return the initial initiator of an action as described above.
     */
    public static ActionInitiator getInitialInitiator(final ActionDescriptionOrBuilder actionDescription) {
        if (actionDescription.getActionCauseList().isEmpty()) {
            return actionDescription.getActionInitiator();
        } else {
            return actionDescription.getActionCause(actionDescription.getActionCauseCount() - 1).getActionInitiator();
        }
    }

    /**
     * Method generates a description for the given action chain.
     *
     * @param actionDescriptionCollection a collection of depending action descriptions.
     *
     * @return a human readable description of the action pipeline.
     */
    public static String getDescription(final Collection<ActionDescription> actionDescriptionCollection) {
        String description = "";
        for (ActionDescription actionDescription : actionDescriptionCollection) {
            if (!description.isEmpty()) {
                description += " > ";
            }
            description += actionDescription.getDescription();
        }
        return description;
    }


//    /**
//     * Generates an action description according to the configuration of this unit remote.
//     * The action description is generated using the ActionDescriptionProcessor.
//     * Additionally the initiator and the authority is detected by using the session manager as well as the user id is properly configured.
//     *
//     * @param serviceAttribute the service attribute that will be applied by this action
//     * @param serviceType      the service type according to the service attribute
//     * @param authorized       flag to define if this action should be authorized by the currently authenticated user or should be performed with OTHER rights.
//     *
//     * @return the generated action description
//     *
//     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
//     *                                  verified or serialized
//     */
//    public static ActionDescription.Builder generateActionDescriptionBuilder(final Message serviceAttribute, final ServiceType serviceType, final boolean authorized) throws CouldNotPerformException {
//        final ActionDescription.Builder actionDescriptionBuilder = ActionDescriptionProcessor.generateActionDescriptionBuilder(serviceAttribute, serviceType);
//        actionDescriptionBuilder.setActionInitiator(detectActionInitiator(authorized));
//        return updateActionDescription(actionDescriptionBuilder, serviceAttribute, serviceType);
//    }
//

    /**
     * Generates an action description according to the configuration of this unit remote.
     * The action description is generated using the ActionDescriptionProcessor.
     * Additionally the initiator and the authority is detected by using the session manager as well as the user id is properly configured.
     *
     * @param serviceAttribute the service attribute that will be applied by this action
     * @param serviceType      the service type according to the service attribute
     *
     * @return the generated action description
     *
     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
     *                                  verified or serialized
     */
    public static ActionDescription.Builder generateActionDescriptionBuilder(final Message serviceAttribute, final ServiceType serviceType) throws CouldNotPerformException {
        return generateActionDescriptionBuilder(generateDefaultActionParameter(serviceAttribute, serviceType));
    }
//
//    /**
//     * Generates an action description according to the given attributes.
//     * The action description is generated using the ActionDescriptionProcessor.
//     * Additionally the initiator and the authority is detected by using the session manager as well as the user id is properly configured.
//     *
//     * @param serviceAttribute the service attribute that will be applied by this action
//     * @param serviceType      the service type according to the service attribute
//     * @param unitType         the service type according to the service attribute
//     * @param authorized       flag to define if this action should be authorized by the currently authenticated user or should be performed with OTHER rights.
//     *
//     * @return the generated action description
//     *
//     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
//     *                                  verified or serialized
//     */
//    public static ActionDescription.Builder generateActionDescriptionBuilder(final Message serviceAttribute, ServiceType serviceType, final UnitType unitType, final boolean authorized) throws CouldNotPerformException {
//
//        // generate default description
//        final ActionDescription.Builder actionDescriptionBuilder = generateActionDescriptionBuilder(serviceAttribute, serviceType, authorized);
//
//        // update unit type
//        actionDescriptionBuilder.getServiceStateDescriptionBuilder().setUnitType(unitType);
//
//        // return
//        return actionDescriptionBuilder;
//    }
//

    /**
     * Generates an action description according to the given attributes.
     * The action description is generated using the ActionDescriptionProcessor.
     * Additionally the initiator is detected by using the session manager as well as the user id is properly configured.
     *
     * @param serviceAttribute the service attribute that will be applied by this action
     * @param serviceType      the service type according to the service attribute
     * @param unitType         the service type according to the service attribute
     *
     * @return the generated action description
     *
     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
     *                                  verified or serialized
     */
    public static ActionDescription.Builder generateActionDescriptionBuilder(final Message serviceAttribute, ServiceType serviceType, final UnitType unitType) throws CouldNotPerformException {
        return generateActionDescriptionBuilder(generateDefaultActionParameter(serviceAttribute, serviceType, unitType));
    }
//
//    /**
//     * Generates an action description according to the configuration of the given unit.
//     * The action description is generated using the ActionDescriptionProcessor.
//     * This method will set the service state description according to the service attribute and service type
//     * and replace several keys in the description to make it human readable.
//     * Additionally the initiator and the authority is detected by using the session manager as well as the user id is properly configured.
//     *
//     * @param serviceAttribute the service attribute that will be applied by this action
//     * @param serviceType      the service type according to the service attribute
//     * @param unit             the unit to control.
//     * @param authorized       flag to define if this action should be authrorized by the currently authenticated user.
//     *
//     * @return the generated action description
//     *
//     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
//     *                                  verified or serialized
//     */
//    public static ActionDescription.Builder generateActionDescriptionBuilderAndUpdate(final Message serviceAttribute, final ServiceType serviceType, final Unit<?> unit, final boolean authorized) throws CouldNotPerformException {
//        return updateActionDescription(generateActionDescriptionBuilder(serviceAttribute, serviceType, authorized), serviceAttribute, serviceType, unit);
//    }
//

    /**
     * Generates an action description according to the configuration of the given unit.
     * The action description is generated using the ActionDescriptionProcessor.
     * This method will set the service state description according to the service attribute and service type.
     * Additionally the initiator and the authority is detected by using the session manager as well as the user id is properly configured.
     *
     * @param serviceAttribute the service attribute that will be applied by this action
     * @param serviceType      the service type according to the service attribute
     * @param unit             the unit to control.
     *
     * @return the generated action description
     *
     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
     *                                  verified or serialized
     */
    public static ActionDescription.Builder generateActionDescriptionBuilder(final Message serviceAttribute, final ServiceType serviceType, final Unit<?> unit) throws CouldNotPerformException {
        return generateActionDescriptionBuilder(generateDefaultActionParameter(serviceAttribute, serviceType, unit));
    }

    /**
     * Generates an {@code ActionDescription} which is based on the given {@code ActionParameter}.
     *
     * @param actionParameter type which contains all needed parameters to generate an {@code ActionDescription}
     *
     * @return an {@code ActionDescription} that only misses unit and service information
     */
    public static ActionDescription.Builder generateActionDescriptionBuilder(final ActionParameterOrBuilder actionParameter) {
        ActionDescription.Builder actionDescription = ActionDescription.newBuilder();

        // add values from ActionParameter
        actionDescription.addAllCategory(actionParameter.getCategoryList());
        actionDescription.setLabel(actionParameter.getLabel());
        actionDescription.setActionInitiator(actionParameter.getActionInitiator());
        actionDescription.setServiceStateDescription(actionParameter.getServiceStateDescription());
        actionDescription.setExecutionTimePeriod(actionParameter.getExecutionTimePeriod());
        actionDescription.setPriority(actionParameter.getPriority());

        // if an initiator action is defined in ActionParameter the actionChain is updated
        if (actionParameter.hasCause()) {
            updateActionCause(actionDescription, actionParameter.getCause());
        }

        return actionDescription;
    }

    /**
     * Update an action description according to the configuration of this unit remote.
     * This method will set the service state description according to the service attribute and service type.
     *
     * @param serviceAttribute the service attribute that will be applied by this action
     * @param serviceType      the service type according to the service attribute
     * @param unit             the unit to control.
     *
     * @return the action description
     *
     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
     *                                  verified or serialized
     */
    public static ServiceStateDescription generateServiceStateDescription(final Message serviceAttribute, final ServiceType serviceType, final Unit<?> unit) throws CouldNotPerformException {
        return generateServiceStateDescription(serviceAttribute, serviceType).setUnitId(unit.getId()).build();
    }

    /**
     * Update an action description according to the configuration of this unit remote.
     * This method will set the service state description according to the service attribute and service type.
     *
     * @param serviceAttribute the service attribute that will be applied by this action
     * @param serviceType      the service type according to the service attribute
     *
     * @return the action description builder
     *
     * @throws CouldNotPerformException if accessing the unit registry fails or if the service attribute cannot be
     *                                  verified or serialized
     */
    public static ServiceStateDescription.Builder generateServiceStateDescription(final Message serviceAttribute, final ServiceType serviceType) throws CouldNotPerformException {
        ServiceStateDescription.Builder serviceStateDescriptionBuilder = ServiceStateDescription.newBuilder();
        serviceStateDescriptionBuilder.setServiceAttribute(JSON_PROCESSOR.serialize(Services.verifyAndRevalidateServiceState(serviceAttribute)));
        serviceStateDescriptionBuilder.setServiceAttributeType(JSON_PROCESSOR.getServiceAttributeType(serviceAttribute));
        serviceStateDescriptionBuilder.setServiceType(serviceType);
        return serviceStateDescriptionBuilder;
    }

    /**
     * Method detects if a human or the system is triggering this action.
     *
     * @return
     */
    @Experimental
    public static ActionInitiator detectActionInitiator(final boolean authorized) {
        final ActionInitiator.Builder actionInitiatorBuilder = ActionInitiator.newBuilder();
        if (authorized && SessionManager.getInstance().isLoggedIn()) {
            if (SessionManager.getInstance().getUserId() != null) {
                actionInitiatorBuilder.setInitiatorId(SessionManager.getInstance().getUserId());
            } else {
                actionInitiatorBuilder.setInitiatorId(SessionManager.getInstance().getClientId());
            }
        } else {
            actionInitiatorBuilder.clearInitiatorId();
        }
        return actionInitiatorBuilder.build();
    }

    /**
     * Prepare an action description. This sets the timestamp, the action initiator type, the id, labels and descriptions.
     *
     * @param actionDescriptionBuilder the action description builder which is prepared.
     * @param unit                     the unit on which the action description is applied.
     *
     * @throws CouldNotPerformException if preparing fails.
     */
    public static void prepare(final ActionDescription.Builder actionDescriptionBuilder, final Unit unit) throws CouldNotPerformException {
        prepare(actionDescriptionBuilder, unit, JSON_PROCESSOR.deserialize(actionDescriptionBuilder.getServiceStateDescription().getServiceAttribute(), actionDescriptionBuilder.getServiceStateDescription().getServiceAttributeType()));
    }

    /**
     * Prepare an action description. This sets the timestamp, the action initiator type, the id, labels and descriptions.
     *
     * @param actionDescriptionBuilder the action description builder which is prepared.
     * @param unit                     the unit on which the action description is applied.
     * @param serviceState             the de-serialized service state as contained in the action description.
     *
     * @throws CouldNotPerformException if preparing fails.
     */
    private static void prepare(final ActionDescription.Builder actionDescriptionBuilder, final Unit unit, final Message serviceState) throws CouldNotPerformException {
        TimestampProcessor.updateTimestampWithCurrentTime(actionDescriptionBuilder);

        // update initiator type
        if (actionDescriptionBuilder.getActionInitiator().hasInitiatorId() && !actionDescriptionBuilder.getActionInitiator().getInitiatorId().isEmpty()) {
            final UnitConfig initiatorUnitConfig = Registries.getUnitRegistry().getUnitConfigById(actionDescriptionBuilder.getActionInitiator().getInitiatorId());
            if ((initiatorUnitConfig.getUnitType() == UnitType.USER && !initiatorUnitConfig.getUserConfig().getSystemUser())) {
                actionDescriptionBuilder.getActionInitiatorBuilder().setInitiatorType(InitiatorType.HUMAN);
            } else {
                actionDescriptionBuilder.getActionInitiatorBuilder().setInitiatorType(InitiatorType.SYSTEM);
            }
        } else if (!actionDescriptionBuilder.getActionInitiator().hasInitiatorType()) {
            // if no initiator is defined than use the system as initiator.
            actionDescriptionBuilder.getActionInitiatorBuilder().setInitiatorType(InitiatorType.SYSTEM);
        }

        // prepare
        actionDescriptionBuilder.setId(UUID.randomUUID().toString());
        LabelProcessor.addLabel(actionDescriptionBuilder.getLabelBuilder(), Locale.ENGLISH, GENERIC_ACTION_LABEL);

        // generate or update action description
        generateDescription(actionDescriptionBuilder, serviceState, unit);
    }

    /**
     * Verify an action description. This triggers an internal call to {@link #verifyActionDescription(Builder, Unit, boolean)}
     * with prepare set to false.
     *
     * @param actionDescription the action description which is verified.
     * @param unit              the unit on which the action description is applied.
     *
     * @return a de-serialized and updated service state.
     *
     * @throws VerificationFailedException if verifying the action description failed.
     */
    public static Message verifyActionDescription(final ActionDescriptionOrBuilder actionDescription, final Unit unit) throws VerificationFailedException {
        ActionDescription.Builder actionDescriptionBuilder;
        if (actionDescription instanceof ActionDescription.Builder) {
            actionDescriptionBuilder = (ActionDescription.Builder) actionDescription;
        } else {
            actionDescriptionBuilder = ((ActionDescription) actionDescription).toBuilder();
        }
        return verifyActionDescription(actionDescriptionBuilder, unit, false);
    }

    /**
     * Verify an action description. If the prepare flag is set to true, the method {@link #prepare(Builder, Unit, Message)}
     * is called to update the action description. Therefore, this method only allows to verify a builder.
     * In addition, this method returns a de-serialized and updated service state contained in the action description.
     * The reason for this is to minimize de-serializing operations because verifying a service state also updates it.
     *
     * @param actionDescriptionBuilder the action description builder which is verified and updated if prepare is set.
     * @param unit                     the unit on which the action description is applied.
     * @param prepare                  flag determining if the action description should be prepared.
     *
     * @return a de-serialized and updated service state.
     *
     * @throws VerificationFailedException if verifying the action description failed.
     */
    public static Message verifyActionDescription(final ActionDescription.Builder actionDescriptionBuilder, final Unit unit, final boolean prepare) throws VerificationFailedException {
        try {
            if (actionDescriptionBuilder == null) {
                throw new NotAvailableException("ActionDescription");
            }

            if (!actionDescriptionBuilder.hasServiceStateDescription()) {
                throw new NotAvailableException("ActionDescription.ServiceStateDescription");
            }

            if (!actionDescriptionBuilder.getServiceStateDescription().hasUnitId() || actionDescriptionBuilder.getServiceStateDescription().getUnitId().isEmpty()) {
                throw new NotAvailableException("ActionDescription.ServiceStateDescription.UnitId");
            }

            if (!actionDescriptionBuilder.getServiceStateDescription().getUnitId().equals(unit.getId())) {
                String targetUnitLabel;
                try {
                    targetUnitLabel = LabelProcessor.getBestMatch(Registries.getUnitRegistry().getUnitConfigById(actionDescriptionBuilder.getServiceStateDescription().getUnitId()).getLabel());
                } catch (CouldNotPerformException ex) {
                    targetUnitLabel = actionDescriptionBuilder.getServiceStateDescription().getUnitId();
                }
                throw new InvalidStateException("Referred Unit["+targetUnitLabel+"] is not compatible with the registered UnitController["+unit.getLabel("?")+"]!");
            }

            for (ActionReference actionReference : actionDescriptionBuilder.getActionCauseList()) {
                if (!actionReference.hasActionId() || actionReference.getActionId().isEmpty()) {
                    throw new InvalidStateException("Action is caused by an unidentifiable action [" + actionReference + "] (id is missing)");
                }
            }

            Message serviceState = JSON_PROCESSOR.deserialize(actionDescriptionBuilder.getServiceStateDescription().getServiceAttribute(), actionDescriptionBuilder.getServiceStateDescription().getServiceAttributeType());
            serviceState = Services.verifyAndRevalidateServiceState(serviceState);
            if (prepare) {
                prepare(actionDescriptionBuilder, unit, serviceState);
            }
            return serviceState;
        } catch (CouldNotPerformException ex) {
            throw new VerificationFailedException("Given ActionDescription[" + actionDescriptionBuilder + "] is invalid!", ex);
        }
    }

    /**
     * Generate a description for an action description. Descriptions are generated as defined in {@link #GENERIC_ACTION_DESCRIPTION_MAP}.
     *
     * @param actionDescriptionBuilder the action description builder in which descriptions are generated.
     * @param serviceState             the de-serialized service state as contained in the action description.
     * @param unit                     the unit on which the action is applied.
     */
    private static void generateDescription(final ActionDescription.Builder actionDescriptionBuilder, final Message serviceState, final Unit unit) {
        final MultiLanguageText.Builder multiLanguageTextBuilder = MultiLanguageText.newBuilder();
        for (Entry<String, String> languageDescriptionEntry : GENERIC_ACTION_DESCRIPTION_MAP.entrySet()) {
            String description = languageDescriptionEntry.getValue();
            try {
                // setup unit label
                description = description.replace(UNIT_LABEL_KEY, unit.getLabel());

                // setup service type
                description = description.replace(SERVICE_TYPE_KEY,
                        StringProcessor.transformToCamelCase(actionDescriptionBuilder.getServiceStateDescription().getServiceType().name()));

                // setup initiator
                if (actionDescriptionBuilder.getActionInitiator().hasInitiatorId() && !actionDescriptionBuilder.getActionInitiator().getInitiatorId().isEmpty()) {
                    description = description.replace(INITIATOR_KEY, LabelProcessor.getBestMatch(Registries.getUnitRegistry().getUnitConfigById(actionDescriptionBuilder.getActionInitiator().getInitiatorId()).getLabel()));
                } else {
                    description = description.replace(INITIATOR_KEY, "Other");
                }

                // setup service attribute
                description = description.replace(SERVICE_ATTRIBUTE_KEY,
                        StringProcessor.transformCollectionToString(Services.generateServiceStateStringRepresentation(serviceState, actionDescriptionBuilder.getServiceStateDescription().getServiceType()), " "));

                // format
                description = StringProcessor.formatHumanReadable(description);

                // generate
                multiLanguageTextBuilder.addEntry(MapFieldEntry.newBuilder().setKey(languageDescriptionEntry.getKey()).setValue(description).build());
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not generate action description!", ex, LOGGER);
            }
            actionDescriptionBuilder.setDescription(multiLanguageTextBuilder);
        }
    }
}
