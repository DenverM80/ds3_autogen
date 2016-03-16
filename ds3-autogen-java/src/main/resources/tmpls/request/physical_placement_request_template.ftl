<#include "../copyright.ftl"/>

package ${packageName};

import com.spectralogic.ds3client.networking.HttpVerb;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.ds3client.models.bulk.Ds3ObjectList;
import com.spectralogic.ds3client.serializer.XmlOutput;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
<#include "../imports.ftl"/>

public class ${name} extends AbstractRequest {

    // Variables
    <#include "common/variables.ftl"/>
    private long size = 0;

    // Constructor
    <#include "common/constructor.ftl"/>

    <#include "common/with_constructors.ftl"/>

    @Override
    public InputStream getStream() {
        final Ds3ObjectList objects = new Ds3ObjectList();
        objects.setObjects(this.objects);

        ${javaHelper.toXmlLine("xmlOutput", "objects", operation)}

        final byte[] stringBytes = xmlOutput.getBytes();
        this.size = stringBytes.length;
        return new ByteArrayInputStream(stringBytes);
    }

    @Override
    public long getSize() {
        return this.size;
    }

    <#include "common/getters_verb_path.ftl"/>

    <#include "common/getters.ftl"/>

}