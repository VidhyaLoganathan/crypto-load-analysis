
package com.cypher.cardload.model;


import lombok.Data;

@Data
public class Token {
    private String address;
    private String symbol;
    private String name;
    private int decimals;
}