package com.cypher.cardload.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.web3j.abi.datatypes.Address;

import java.time.LocalDate;
import java.util.Objects;

@Getter
@AllArgsConstructor
public class TokenDayKey {
    private final Address token;
    private final LocalDate date;
    @Override public int hashCode() { return Objects.hash(token, date); }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TokenDayKey)) return false;
        TokenDayKey k = (TokenDayKey) o; return token.equals(k.token) && date.equals(k.date);
    }
}
