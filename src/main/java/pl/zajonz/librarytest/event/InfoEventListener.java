package pl.zajonz.librarytest.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pl.zajonz.librarytest.event.model.InfoEvent;
import pl.zajonz.librarytest.service.MessageSender;

@Service
@RequiredArgsConstructor
public class InfoEventListener {

    private final MessageSender messageSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleInfoEvent(InfoEvent event) {
        messageSender.sendInfo(event.getInfoMessage());
    }

}
