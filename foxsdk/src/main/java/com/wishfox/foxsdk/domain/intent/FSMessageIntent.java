package com.wishfox.foxsdk.domain.intent;

import java.util.Objects;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 19:01
 */
public abstract class FSMessageIntent implements FoxSdkViewIntent {

    public static class Refresh extends FSMessageIntent {
        public Refresh() {}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o != null && getClass() == o.getClass();
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public String toString() {
            return "Refresh{}";
        }
    }

    public static class Read extends FSMessageIntent {
        private Long id;

        public Read() {
            this.id = null;
        }

        public Read(Long id) {
            this.id = id;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Read read = (Read) o;
            return Objects.equals(id, read.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "Read{" +
                    "id=" + id +
                    '}';
        }
    }
}
