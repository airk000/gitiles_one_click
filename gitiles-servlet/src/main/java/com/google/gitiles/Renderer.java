// Copyright 2012 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gitiles;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.template.soy.tofu.SoyTofu;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/** Renderer for Soy templates used by Gitiles. */
public abstract class Renderer {
  private static final List<String> SOY_FILENAMES = ImmutableList.of(
      "Common.soy",
      "DiffDetail.soy",
      "HostIndex.soy",
      "LogDetail.soy",
      "ObjectDetail.soy",
      "PathDetail.soy",
      "RefList.soy",
      "RevisionDetail.soy",
      "RepositoryIndex.soy");

  public static final Map<String, String> STATIC_URL_GLOBALS = ImmutableMap.of(
      "gitiles.CSS_URL", "gitiles.css",
      "gitiles.PRETTIFY_CSS_URL", "prettify/prettify.css",
      "gitiles.PRETTIFY_JS_URL", "prettify/prettify.js");

  protected static final URL toFileURL(String filename) {
    if (filename == null) {
      return null;
    }
    try {
      return new File(filename).toURI().toURL();
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
  }

  protected ImmutableList<URL> templates;
  protected ImmutableMap<String, String> globals;

  protected Renderer(Function<String, URL> resourceMapper, Map<String, String> globals,
      String staticPrefix, URL customTemplates, String siteTitle) {
    checkNotNull(staticPrefix, "staticPrefix");
    List<URL> allTemplates = Lists.newArrayListWithCapacity(SOY_FILENAMES.size() + 1);
    for (String filename : SOY_FILENAMES) {
      allTemplates.add(resourceMapper.apply(filename));
    }
    if (customTemplates != null) {
      allTemplates.add(customTemplates);
    } else {
      allTemplates.add(resourceMapper.apply("DefaultCustomTemplates.soy"));
    }
    templates = ImmutableList.copyOf(allTemplates);

    Map<String, String> allGlobals = Maps.newHashMap();
    for (Map.Entry<String, String> e : STATIC_URL_GLOBALS.entrySet()) {
      allGlobals.put(e.getKey(), staticPrefix + e.getValue());
    }
    allGlobals.put("gitiles.SITE_TITLE", siteTitle);
    allGlobals.putAll(globals);
    this.globals = ImmutableMap.copyOf(allGlobals);
  }

  public void render(HttpServletResponse res, String templateName) throws IOException {
    render(res, templateName, ImmutableMap.<String, Object> of());
  }

  public void render(HttpServletResponse res, String templateName, Map<String, ?> soyData)
      throws IOException {
    res.setContentType("text/html");
    res.setCharacterEncoding("UTF-8");
    byte[] data = newRenderer(templateName).setData(soyData).render().getBytes(Charsets.UTF_8);
    res.setContentLength(data.length);
    res.getOutputStream().write(data);
  }

  SoyTofu.Renderer newRenderer(String templateName) {
    return getTofu().newRenderer(templateName);
  }

  protected abstract SoyTofu getTofu();
}
