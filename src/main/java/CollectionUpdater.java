import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @param <E> Тип элементов обновляемой коллекции
 * @param <D> Тип элементов в целевой коллекции-образце
 * @param <K> Тип для ключей, по которым сопостовляются элементы обновляемой колллекции
 *           и целевой коллекции-образца.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class CollectionUpdater<E, D, K> {

    private final Function<E, K> currentKey;
    private final Function<D, K> intendedKey;

    private Check4Changes<E, D> check4Changes;

    private Consumer<D> createElement      = CollectionUpdater::ignore;
    private Consumer<E> deleteElement      = CollectionUpdater::ignore;
    private BiConsumer<E, D> updateElement = CollectionUpdater::ignoreUpdate;


    public CollectionUpdater(Function<E, K> currentKey, Function<D, K> intendedKey) {
        this.currentKey = currentKey;
        this.intendedKey = intendedKey;
    }


    /**
     * Метод позволяет установить функцию, которая умеет проверять, содержит ли целевой
     * объект отличия от исходного элемента. Если такая функция назначена, она будет
     * проверяться перед вызовом {@link #updateElement}, а вызов будет выполнен только в том
     * случае, если функция вернёт true, что означает наличие изменений по сравнению
     * с исходным состоянием элемента.<br/><br/>
     * Если функция не назначена, {@link #updateElement} будет выполняться всегда, безусловно.
     */
    public CollectionUpdater<E, D, K> check4Changes(Check4Changes<E, D> check4Changes) {
        this.check4Changes = check4Changes;
        return this;
    }


    public CollectionUpdater<E, D, K> createElement(Consumer<D> createElement) {
        this.createElement = createElement;
        return this;
    }

    public CollectionUpdater<E, D, K> updateElement(BiConsumer<E, D> updateElement) {
        this.updateElement = updateElement;
        return this;
    }

    public CollectionUpdater<E, D, K> deleteElement(Consumer<E> deleteElement) {
        this.deleteElement = deleteElement;
        return this;
    }


    public void collection(Collection<E> current, Collection<D> intended) {
        collection(current, intended.stream());
    }

    public void collection(Collection<E> current, Stream<D> intended) {
        collection(current.stream(), intended);
    }

    public void collection(Stream<E> current, Stream<D> intended) {

        Map<K, E> map = current.collect(Collectors.toMap(currentKey, element -> element));

        intended.forEach(intendedElement -> {
            K key = intendedKey.apply(intendedElement);
            E currentElement = map.get(key);

            if (currentElement == null) {

                createElement.accept(intendedElement);

            } else {

                if (check4Changes == null || check4Changes.isChanged(currentElement, intendedElement)) {
                    updateElement.accept(currentElement, intendedElement);
                }

                map.remove(key);

            }

        });

        map.values().forEach(deleteElement);

    }

    private static <ED> void ignore(ED intended) {}
    private static <E, D> void ignoreUpdate(E current, D intended) {}


    @FunctionalInterface
    public interface Check4Changes<E, D> {
        boolean isChanged(E element, D intended);
    }


    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class CollectionUpdaterBuilder<E, K> {
        private final Function<E, K> currentKey;
        public <D> CollectionUpdater<E, D, K> intendedElementKey(Function<D, K> intendedKey) {
            return new CollectionUpdater<>(currentKey, intendedKey);
        }
    }

    public static <E, K> CollectionUpdaterBuilder<E, K> currentElementKey(Function<E, K> currentKey) {
        return new CollectionUpdaterBuilder<>(currentKey);
    }

}