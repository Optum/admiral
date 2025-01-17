package com.optum.admiral.model.dockerjava;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.PullResponseItem;
import com.optum.admiral.exception.AdmiralDockerException;
import com.optum.admiral.model.DockerModelController;
import com.optum.admiral.model.ProgressHandler;

public class DockerJavaPullCallback extends ResultCallback.Adapter<PullResponseItem> {
    private final Thread blockingThread;
    private final DockerModelController dmc;
    private final ProgressHandler progressHandler;
    private boolean receivedPullSuccessful = false;

    public DockerJavaPullCallback(Thread blockingThread, DockerModelController dmc, ProgressHandler progressHandler) {
        this.blockingThread = blockingThread;
        this.dmc = dmc;
        this.progressHandler = progressHandler;
    }

    public boolean successedDetected() {
        return receivedPullSuccessful;
    }

    /** Called when an async result event occurs */
    @Override
    public void onNext(PullResponseItem object) {
        if (object.isPullSuccessIndicated()) {
            receivedPullSuccessful = true;
        }
        DockerJavaProgressMessage dockerJavaProgressMessage = new DockerJavaProgressMessage(object);
        try {
            progressHandler.progress(dockerJavaProgressMessage);
        } catch (AdmiralDockerException e) {
            dmc.publishUnhandledException(e);
        }
    }

    /** Called when an exception occurs while processing */
    @Override
    public void onError(Throwable throwable) {
        if (throwable instanceof InternalServerErrorException) {
            InternalServerErrorException isee = (InternalServerErrorException) throwable;
            dmc.parseCauseOfInternalServerErrorException(isee);
        } else if (throwable instanceof NotFoundException) {
            NotFoundException nfe = (NotFoundException) throwable;
            dmc.parseCauseOfNotFoundException(nfe);
        } else {
            dmc.publishUnhandledException(throwable);
        }
        if (blockingThread!=null) {
            blockingThread.interrupt();
        }
    }

}
