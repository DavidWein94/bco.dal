/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.service.HardwareManager;
import de.citec.dal.service.rsb.RSBCommunicationService;
import de.citec.dal.service.rsb.RSBRemoteService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.RSBException;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.EventCallback;
import rsb.patterns.LocalServer;
import rst.homeautomation.openhab.DALBindingType;
import rst.homeautomation.openhab.DALBindingType.DALBinding;
import rst.homeautomation.openhab.OpenhabCommandType;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand;
import rst.homeautomation.openhab.RSBBindingType;
import rst.homeautomation.openhab.RSBBindingType.RSBBinding;

/**
 * @author thuxohl
 * @author mpohling
 */
public class OpenhabBinding implements OpenhabBindingInterface {

	public static final Scope SCOPE_DAL = new Scope("/dal");
	public static final Scope SCOPE_OPENHAB = new Scope("/openhab");

	private static final Logger logger = LoggerFactory.getLogger(OpenhabBinding.class);

	private static OpenhabBinding instance;

    private final RSBRemoteService<RSBBinding> openhabRemoteService;
    private final RSBCommunicationService<DALBinding, DALBinding.Builder> dalService;
	private final HardwareManager hardwareManager;

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(OpenhabCommandType.OpenhabCommand.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(RSBBindingType.RSBBinding.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(DALBindingType.DALBinding.getDefaultInstance()));
    }

	public synchronized static OpenhabBinding getInstance() {
		if(instance == null) {
			instance = new OpenhabBinding();
		}
		return instance;
	}

    private OpenhabBinding() {
		this.hardwareManager = HardwareManager.getInstance();

        openhabRemoteService = new RSBRemoteService<RSBBinding>() {

            @Override
            public void notifyUpdated(RSBBinding data) {
                OpenhabBinding.this.notifyUpdated(data);
            }
        };
        openhabRemoteService.init(SCOPE_OPENHAB);

        dalService = new RSBCommunicationService<DALBinding, DALBinding.Builder>(SCOPE_DAL, DALBinding.newBuilder()) {

            @Override
            public void registerMethods(LocalServer server) throws RSBException {
                OpenhabBinding.this.registerMethods(server);
            }
        };
        try {
            dalService.init();
        } catch (RSBException ex) {
            logger.warn("Unable to initialize the communication service in [" + getClass().getSimpleName() + "]");
        }

        

        openhabRemoteService.activate();
        {
            try {
                dalService.activate();
            } catch (Exception ex) {
                logger.warn("Unable to activate the communication service in [" + getClass().getSimpleName() + "]", ex);
            }
        }
    }

    public final void notifyUpdated(RSBBinding data) {
        switch (data.getState().getState()) {
            case ACTIVE:
                logger.debug("Active rsb binding state!");
                break;
            case DEACTIVE:
                logger.debug("Deactive rsb binding state!");
                break;
            case UNKNOWN:
                logger.debug("Unkown rsb binding state!");
                break;
        }
    }

    public final void registerMethods(LocalServer server) {
        try {
            server.addMethod("internalReceiveUpdate", new InternalReceiveUpdateCallback());
        } catch (RSBException ex) {
            logger.warn("Could not add methods to local server in [" + getClass().getSimpleName() + "]", ex);
        }
    }

    @Override
    public void internalReceiveUpdate(OpenhabCommand command) {
        hardwareManager.internalReceiveUpdate(command);
    }

    public static class InternalReceiveUpdateCallback extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            OpenhabBinding.instance.internalReceiveUpdate((OpenhabCommand) request.getData());
            return new Event(String.class, "Ok");
        }
    }

    @Override
    public Future executeCommand(OpenhabCommandType.OpenhabCommand command) throws RSBBindingException {
		try {
			openhabRemoteService.callMethod("executeCommand", command);
			return null; // TODO: mpohling implement future handling.
		} catch (RSBException | ExecutionException | TimeoutException  ex) {
			throw new RSBBindingException("Could not execute "+command+"!", ex);
		}
    }
}