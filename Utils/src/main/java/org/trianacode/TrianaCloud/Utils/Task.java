/*
 *
 *  * Copyright - TrianaCloud
 *  * Copyright (C) 2012. Kieran Evans. All Rights Reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or
 *  * modify it under the terms of the GNU General Public License
 *  * as published by the Free Software Foundation; either version 2
 *  * of the License, or (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.trianacode.TrianaCloud.Utils;

import java.util.Date;

/**
 * Task encapsulates a task (or job), and any relevant information (e.g. where to send results).
 * TODO: A toWire method that converts the necessary data to some wire format (e.g. ASN.1). Keep it simple.
 */
public class Task {
    ///TODO: Task metadata class.
    public String origin;
    public byte[] data;
    public long dispatchTime;
    public String routingKey;

    public Task(String o, byte[] d, String r) {
        origin = o;
        data = d;
        routingKey = r;
        dispatchTime = System.currentTimeMillis();
    }

    public Date totalTime() {
        return new Date(System.currentTimeMillis() - dispatchTime);
    }

    public byte[] getData() {
        return data;
    }
}