/*
 * Copyright 2013 University of Chicago and Argonne National Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

/** JSON.SWIFT
 *
 *  JSON functionality for Swift/T- relies on Python
 *  See turbine_helpers.py for implementations
 */

import python;

(string t) json_type(file f, string path)
{
  wait (f)
  {
    t = python_persist("from turbine_helpers import *",
                       "json_type('%s','%s')" %
                       (filename(f), path));
  }
}

(string t) json_list_length(file f, string path)
{
  wait (f)
  {
    t = python_persist("from turbine_helpers import *",
                       "json_list_length('%s','%s')" %
                       (filename(f), path));
  }
}

(string t) json_get(string J, string path)
{
    t = python_persist("from turbine_helpers import *",
                       "json_get('%s','%s')" %
                       (J, path));
}

(float t) json_get_float(string J, string path)
{
  s = json_get(J, path);
  t = string2float(s);
}

(int t) json_get_int(string J, string path)
{
  s = json_get(J, path);
  t = string2int(s);
}

(string t) json_dict_entries(file f, string path)
{
  wait (f)
  {
    t = python_persist("from turbine_helpers import *",
                       "json_dict_entries('%s','%s')" %
                       (filename(f), path));
  }
}

(string o) json_arrayify(string text)
{
  o = "[" + text + "]";
}

(string o) json_objectify(string text)
{
  o = "{" + text + "}";
}

(string o) json_encode_array(int|float|string|boolean... args)
"turbine" "1.2.3" "json_encode_array";

// + _contents

(string o) json_encode_array_retype(string types[],
                                    int|float|string|boolean... args)
"turbine" "1.2.3" "json_encode_array_retype";

// + _contents

(string o) json_encode_array_format(string format,
                                    int|float|string|boolean... args)
"turbine" "1.2.3" "json_encode_array_format";

// + _contents

(string o) json_encode_object(string names[], int|float|string|boolean... args)
"turbine" "1.2.3" "json_encode_object";

// + _contents

(string o) json_encode_object_retype(string names[], string types[],
                                     int|float|string|boolean... args)
"turbine" "1.2.3" "json_encode_object_retype";

// + _contents

(string o) json_encode_object_format(string format,
                                    int|float|string|boolean... args)
"turbine" "1.2.3" "json_encode_object_format";

// + _contents
