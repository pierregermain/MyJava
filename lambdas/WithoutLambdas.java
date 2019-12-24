package lambdas;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import lambdas.Person;

public class WithoutLambdas {

    public static void main(String[] args) {

        List<Person> people = Arrays.asList(
                new Person ("Miguel", "Cactics", 39),
                new Person ("Pierre", "Cert", 39),
                new Person ("Fer", "Xio", 39),
                new Person ("Rafa", "Laga", 39)
        );

        // ----------------
        // WITHOUT LAMBDAS
        // ----------------

        // Sort list by last name
        Collections.sort(people, new Comparator<Person>() {
            @Override
            public int compare(Person o1, Person o2) {
                return o1.getLastname().compareTo(o2.getLastname());
            }
        });

        // Print all elements in the list
        for (int i = 0; i < people.size(); i++) {
            System.out.println(people.get(i));
        }

        // Print last name beginning with C
        for (int i = 0; i < people.size(); i++) {
            Person person = people.get(i);
            String lastName = person.getLastname();
            if (lastName.startsWith("C")) {
                System.out.println(people.get(i));
            }
        }

        // ----------------
        // WITH CONDITIONS
        // ----------------
        // Print last name beginning with X using a Comparator
        printConditionally(people, new Condition() {
            @Override
            public boolean test(lambdas.Person p) {
                return p.getLastname().startsWith("X");
            }
        });
    }

    private static void printConditionally(List<Person> people, Condition condition){
        for (Person p : people) {
            if (condition.test(p)){
                System.out.println(p);
            }
        }

    }
}

interface Condition {
    boolean test(lambdas.Person p);
}
