package de.samply.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Display metadata for forms, fields, and blocks that can additionally provide trusted content
 * before and after the corresponding UI element. Groups and allowed values deliberately continue
 * to use {@link DisplayMetadata}, so {@code pre_info}/{@code post_info} are not supported there.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class ContextualDisplayMetadata extends DisplayMetadata {

    @JsonProperty("pre_info")
    private DisplayInfo preInfo;

    @JsonProperty("post_info")
    private DisplayInfo postInfo;

    @Override
    public ContextualDisplayMetadata fetchDisplayMetadata() {
        ContextualDisplayMetadata result = new ContextualDisplayMetadata();
        result.setDisplayName(getDisplayName());
        result.setDescription(getDescription());
        result.setShortDescription(getShortDescription());
        result.setPreInfo(preInfo);
        result.setPostInfo(postInfo);
        return result;
    }

}
