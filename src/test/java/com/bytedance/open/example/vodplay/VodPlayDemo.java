package com.bytedance.open.example.vodplay;

import com.bytedance.open.service.vod.VodService;
import com.bytedance.open.service.vod.impl.VodServiceImpl;

import java.util.HashMap;
import java.util.Map;

public class VodPlayDemo {

    public static void main(String[] args) {
        VodService vodService = VodServiceImpl.getInstance();

        Map<String, String> query = new HashMap<String, String>();
        query.put("video_id", "your vid");

//        vodService.setAccessKey("your ak");
//        vodService.setSecretKey("your sk");
        try {
            com.bytedance.open.model.vod.GetPlayInfoResp resp = vodService.getPlayInfo(query);
            if (resp.getResponseMetadata().getError() != null) {
                System.out.println(resp.getResponseMetadata().getError());
            }
            System.out.println(resp.getResult());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}