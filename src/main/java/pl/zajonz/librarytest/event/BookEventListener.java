package pl.zajonz.librarytest.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pl.zajonz.librarytest.event.model.BookEvent;
import pl.zajonz.librarytest.service.MessageSender;

@Service
@RequiredArgsConstructor
public class BookEventListener {

    private final MessageSender messageSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookEvent(BookEvent event) {
        messageSender.sendEmailInfo(event.getBook());
    }


}
