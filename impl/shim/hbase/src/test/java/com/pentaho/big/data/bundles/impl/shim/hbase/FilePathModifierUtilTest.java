/*******************************************************************************
 *
 * Pentaho Big Data
 *
 * Copyright (C) 2002-2018 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package com.pentaho.big.data.bundles.impl.shim.hbase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FilePathModifierUtilTest {
  @Test
  public void testModifyPathToConfigFileIfNecessaryWhenPathToFileNullShouldReturnEmptyPath() {
    String pathToFile = null;

    String expectedPathToFile = "";
    String actualPathToFile = FilePathModifierUtil.modifyPathToConfigFileIfNecessary( pathToFile );

    assertEquals( expectedPathToFile, actualPathToFile );
  }

  @Test
  public void testModifyPathToConfigFileIfNecessaryWhenPathToFileLinuxSpecificNullShouldReturnNotModifiedPath() {
    String pathToFile = "/linux/specific";

    String expectedPathToFile = pathToFile;
    String actualPathToFile = FilePathModifierUtil.modifyPathToConfigFileIfNecessary( pathToFile );

    assertEquals( expectedPathToFile, actualPathToFile );
  }

  @Test
  public void testModifyPathToConfigFileIfNecessaryWhenPathToFileWindowsSpecificNullShouldReturnModifiedPath() {
    String pathToFile = "D:\\windows\\specific";

    String expectedPathToFile = "file:///".concat( pathToFile );
    String actualPathToFile = FilePathModifierUtil.modifyPathToConfigFileIfNecessary( pathToFile );

    assertEquals( expectedPathToFile, actualPathToFile );
  }

  @Test
  public void testModifyPathToConfigFileIfNecessaryWhenPathToFileHasSchemePrefixShouldReturnNotModifiedPath() {
    String pathToFile = "scheme://path/to/file";

    String expectedPathToFile = pathToFile;
    String actualPathToFile = FilePathModifierUtil.modifyPathToConfigFileIfNecessary( pathToFile );

    assertEquals( expectedPathToFile, actualPathToFile );
  }
}