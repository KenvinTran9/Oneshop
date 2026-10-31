package com.oneshop.dto.request;

import jakarta.validation.constraints.*;
public class StoreForm {
    @NotBlank(message="Vui lòng nhập mã cửa hàng.") @Pattern(regexp="[A-Za-z0-9_-]{1,30}",message="Mã cửa hàng tối đa 30 ký tự, gồm chữ, số, gạch ngang hoặc gạch dưới.") private String code;
    @NotBlank(message="Vui lòng nhập tên cửa hàng.") @Size(max=150,message="Tên cửa hàng tối đa 150 ký tự.") private String name;
    @NotBlank(message="Vui lòng nhập địa chỉ.") @Size(max=255,message="Địa chỉ tối đa 255 ký tự.") private String address;
    @Size(max=80,message="Quận/huyện tối đa 80 ký tự.") private String district;
    @Size(max=80,message="Tỉnh/thành phố tối đa 80 ký tự.") private String city;
    @Size(max=20,message="Điện thoại tối đa 20 ký tự.") @Pattern(regexp="[0-9+() .-]*",message="Số điện thoại không hợp lệ.") private String phone;
    @NotNull(message="Vui lòng tải lại biểu mẫu cửa hàng.") private Long version;
    private boolean active = true;
    public String getCode(){return code;} public void setCode(String v){code=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getAddress(){return address;} public void setAddress(String v){address=v;}
    public String getDistrict(){return district;} public void setDistrict(String v){district=v;}
    public String getCity(){return city;} public void setCity(String v){city=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public Long getVersion(){return version;} public void setVersion(Long v){version=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
}
