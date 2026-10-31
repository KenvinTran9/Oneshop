package com.oneshop.service;

import com.oneshop.security.StoreAccess;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
@PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
public class SalesReportService {
    public record StoreSales(String name,long orders,BigDecimal revenue){}
    public record ProductSales(String name,long quantity,BigDecimal revenue){}
    public record DaySales(String day,BigDecimal revenue,int percent){}
    public record Report(LocalDate from,LocalDate to,Long storeId,BigDecimal revenue,long completed,long cancelled,
                         List<StoreSales> stores,List<ProductSales> products,List<DaySales> days){}
    private final NamedParameterJdbcTemplate jdbc;
    private final StoreAccess access;
    public SalesReportService(NamedParameterJdbcTemplate jdbc,StoreAccess access){this.jdbc=jdbc;this.access=access;}
    public Report report(LocalDate from,LocalDate to,Long requestedStore){
        if(from==null||to==null||to.isBefore(from)||ChronoUnit.DAYS.between(from,to)>365)
            throw new IllegalArgumentException("Chọn khoảng thời gian hợp lệ, tối đa 366 ngày.");
        Long storeId=access.scope(requestedStore);
        var p=new MapSqlParameterSource().addValue("start",from.atStartOfDay()).addValue("end",to.plusDays(1).atStartOfDay()).addValue("store",storeId==null?-1L:storeId);
        // Completion time is an immutable event, not updated_at (which email retries can change).
        String source=" from orders o join order_events e on e.order_id=o.id and e.status='COMPLETED' "
                +"where o.status='COMPLETED' and e.created_at>=:start and e.created_at<:end and (:store=-1 or o.store_id=:store)";
        BigDecimal revenue=jdbc.queryForObject("select coalesce(sum(o.total_amount-coalesce(o.shipping_fee,0)),0)"+source,p,BigDecimal.class);
        Long completed=jdbc.queryForObject("select count(*)"+source,p,Long.class);
        Long cancelled=jdbc.queryForObject("select count(distinct o.id) from orders o join order_events e on e.order_id=o.id and e.status='CANCELLED' where e.created_at>=:start and e.created_at<:end and (:store=-1 or o.store_id=:store)",p,Long.class);
        var branches=jdbc.query("select coalesce((select s.name from stores s where s.id=o.store_id),'Không xác định') name, count(*) orders, sum(o.total_amount-coalesce(o.shipping_fee,0)) revenue"+source+" group by o.store_id order by revenue desc",p,
                (r,n)->new StoreSales(r.getString("name"),r.getLong("orders"),r.getBigDecimal("revenue")));
        var top=jdbc.query("select i.product_name name,sum(i.quantity) quantity,sum(i.line_total) revenue from order_items i join orders o on o.id=i.order_id join order_events e on e.order_id=o.id and e.status='COMPLETED' where o.status='COMPLETED' and e.created_at>=:start and e.created_at<:end and (:store=-1 or o.store_id=:store) group by i.product_id,i.product_name order by quantity desc,revenue desc limit 10",p,
                (r,n)->new ProductSales(r.getString("name"),r.getLong("quantity"),r.getBigDecimal("revenue")));
        var amounts=new LinkedHashMap<String,BigDecimal>();
        jdbc.query("select date(e.created_at) day,sum(o.total_amount-coalesce(o.shipping_fee,0)) revenue"+source+" group by date(e.created_at) order by day",p,
                (org.springframework.jdbc.core.RowCallbackHandler)r->amounts.put(r.getString("day"),r.getBigDecimal("revenue")));
        BigDecimal max=amounts.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        var days=amounts.entrySet().stream().map(e->new DaySales(e.getKey(),e.getValue(),max.signum()==0?0:e.getValue().multiply(BigDecimal.valueOf(100)).divide(max,0,java.math.RoundingMode.HALF_UP).intValue())).toList();
        return new Report(from,to,storeId,revenue,completed,cancelled,branches,top,days);
    }
}
