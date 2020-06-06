package kr.gracelove.querydsl;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.gracelove.querydsl.entity.Hello;
import kr.gracelove.querydsl.entity.QHello;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

    @Autowired
    EntityManager em;

    @Test
    void contextLoads() {
        Hello hello = new Hello();
        em.persist(hello);

        JPAQueryFactory query = new JPAQueryFactory(em); //권장
//        QHello qHello = new QHello("h");
        QHello qHello = QHello.hello;

        Hello result = query
                .selectFrom(qHello)
                .fetchOne();

//        JPAQuery<QHello> query = new JPAQuery<QHello>(em);
//        QHello qHello = new QHello("h");
//
//        Hello result = query
//                .select(qHello)
//                .from(qHello)
//                .fetchOne();

        assertEquals(result, hello);
    }

}
