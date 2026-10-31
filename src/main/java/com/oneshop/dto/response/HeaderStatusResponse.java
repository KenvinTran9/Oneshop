package com.oneshop.dto.response;

public record HeaderStatusResponse(boolean authenticated, String email, boolean isAdmin, long itemCount, boolean isBranchManager) {
    public HeaderStatusResponse(boolean authenticated,String email,boolean isAdmin,long itemCount){this(authenticated,email,isAdmin,itemCount,false);}
}
