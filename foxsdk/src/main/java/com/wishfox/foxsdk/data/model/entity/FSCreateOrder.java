package com.wishfox.foxsdk.data.model.entity;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:53
 */
public class FSCreateOrder {

    private String code_url;
    private String extend;
    private String isspid;
    private String jump_url;
    private String pay_type;
    private String pos_id;
    private String pos_seq;
    private String response_type;
    private Result result;
    private String sign;
    private String sys_seq;
    private String trans_time;
    private String type;
    private String virtual_coin;
    private String trade_type;
    private String trade_number;
    private String busy_type;
    private String busy_code;
    private String busy_title;
    private String busy_amount;
    private String operate_channel;

    public FSCreateOrder() {}

    // Getter和Setter方法
    public String getCode_url() { return code_url; }
    public void setCode_url(String code_url) { this.code_url = code_url; }

    public String getExtend() { return extend; }
    public void setExtend(String extend) { this.extend = extend; }

    public String getIsspid() { return isspid; }
    public void setIsspid(String isspid) { this.isspid = isspid; }

    public String getJump_url() { return jump_url; }
    public void setJump_url(String jump_url) { this.jump_url = jump_url; }

    public String getPay_type() { return pay_type; }
    public void setPay_type(String pay_type) { this.pay_type = pay_type; }

    public String getPos_id() { return pos_id; }
    public void setPos_id(String pos_id) { this.pos_id = pos_id; }

    public String getPos_seq() { return pos_seq; }
    public void setPos_seq(String pos_seq) { this.pos_seq = pos_seq; }

    public String getResponse_type() { return response_type; }
    public void setResponse_type(String response_type) { this.response_type = response_type; }

    public Result getResult() { return result; }
    public void setResult(Result result) { this.result = result; }

    public String getSign() { return sign; }
    public void setSign(String sign) { this.sign = sign; }

    public String getSys_seq() { return sys_seq; }
    public void setSys_seq(String sys_seq) { this.sys_seq = sys_seq; }

    public String getTrans_time() { return trans_time; }
    public void setTrans_time(String trans_time) { this.trans_time = trans_time; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getVirtual_coin() { return virtual_coin; }
    public void setVirtual_coin(String virtual_coin) { this.virtual_coin = virtual_coin; }

    public String getTrade_type() { return trade_type; }
    public void setTrade_type(String trade_type) { this.trade_type = trade_type; }

    public String getTrade_number() { return trade_number; }
    public void setTrade_number(String trade_number) { this.trade_number = trade_number; }

    public String getBusy_type() { return busy_type; }
    public void setBusy_type(String busy_type) { this.busy_type = busy_type; }

    public String getBusy_code() { return busy_code; }
    public void setBusy_code(String busy_code) { this.busy_code = busy_code; }

    public String getBusy_title() { return busy_title; }
    public void setBusy_title(String busy_title) { this.busy_title = busy_title; }

    public String getBusy_amount() { return busy_amount; }
    public void setBusy_amount(String busy_amount) { this.busy_amount = busy_amount; }

    public String getOperate_channel() { return operate_channel; }
    public void setOperate_channel(String operate_channel) { this.operate_channel = operate_channel; }

    /**
     * 结果实体
     */
    public static class Result {
        private String comment;
        private String id;

        public Result() {}

        public Result(String comment, String id) {
            this.comment = comment;
            this.id = id;
        }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }
}
