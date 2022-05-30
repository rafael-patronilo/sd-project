package tp1.common;

public record WithHeader<T>(String name, String value, T object){}
