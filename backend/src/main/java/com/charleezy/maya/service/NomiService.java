package com.charleezy.maya.service;

import com.charleezy.maya.model.dto.NomiMessage;
import com.charleezy.maya.model.dto.NomiResponse;

public interface NomiService {
    /**
     * Send a message to a specific Nomi and get their response
     * @param nomiId the UUID of the Nomi
     * @param message the message to send
     * @return the Nomi's response
     */
    NomiResponse sendMessage(String nomiId, NomiMessage message);

    /**
     * List all Nomis associated with the user's account
     * @return list of Nomis
     */
    java.util.List<NomiResponse.Nomi> listNomis();

    /**
     * Get details about a specific Nomi
     * @param nomiId the UUID of the Nomi
     * @return the Nomi's details
     */
    NomiResponse.Nomi getNomi(String nomiId);
} 