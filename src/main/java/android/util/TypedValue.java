/*
 * Copyright 2008 Android4ME
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package android.util;

/**
 * POJO and enums for typed values.
 * 
 * @author Dmitry Skiba
 */
public class TypedValue {

  public int type;
  public CharSequence string;
  public int data;
  public int assetCookie;
  public int resourceId;
  public int changingConfigurations;

  public static final int TYPE_NULL = 0;
  public static final int TYPE_REFERENCE = 1;
  public static final int TYPE_ATTRIBUTE = 2;
  public static final int TYPE_STRING = 3;
  public static final int TYPE_FLOAT = 4;
  public static final int TYPE_DIMENSION = 5;
  public static final int TYPE_FRACTION = 6;
  public static final int TYPE_FIRST_INT = 16;
  public static final int TYPE_INT_DEC = 16;
  public static final int TYPE_INT_HEX = 17;
  public static final int TYPE_INT_BOOLEAN = 18;
  public static final int TYPE_FIRST_COLOR_INT = 28;
  public static final int TYPE_INT_COLOR_ARGB8 = 28;
  public static final int TYPE_INT_COLOR_RGB8 = 29;
  public static final int TYPE_INT_COLOR_ARGB4 = 30;
  public static final int TYPE_INT_COLOR_RGB4 = 31;
  public static final int TYPE_LAST_COLOR_INT = 31;
  public static final int TYPE_LAST_INT = 31;

  public static final int COMPLEX_UNIT_PX = 0;
  public static final int COMPLEX_UNIT_DIP = 1;
  public static final int COMPLEX_UNIT_SP = 2;
  public static final int COMPLEX_UNIT_PT = 3;
  public static final int COMPLEX_UNIT_IN = 4;
  public static final int COMPLEX_UNIT_MM = 5;
  public static final int COMPLEX_UNIT_SHIFT = 0;
  public static final int COMPLEX_UNIT_MASK = 15;
  public static final int COMPLEX_UNIT_FRACTION = 0;
  public static final int COMPLEX_UNIT_FRACTION_PARENT = 1;
  public static final int COMPLEX_RADIX_23p0 = 0;
  public static final int COMPLEX_RADIX_16p7 = 1;
  public static final int COMPLEX_RADIX_8p15 = 2;
  public static final int COMPLEX_RADIX_0p23 = 3;
  public static final int COMPLEX_RADIX_SHIFT = 4;
  public static final int COMPLEX_RADIX_MASK = 3;
  public static final int COMPLEX_MANTISSA_SHIFT = 8;
  public static final int COMPLEX_MANTISSA_MASK = 0xFFFFFF;

}
