{% extends "struct_function_template.java" %}

    {%- block constructor_simple %}
    public {{class_name}}() { }
    {% endblock -%}

    {%- block setter %}
    {%- for p in params|rejectattr('name') %}

    /**
     * Sets the {{p.origin}}.
     {%- if p.description is defined %}
     {%- for d in p.description %}
     * {{d}}
     {%- endfor %}{%- endif %}
     *
     {% if p.param_doc is not defined -%}
     * @param {{p.last}}
     {% else -%}
     {% set l = p.last|length + 8 -%}
     * {% for v in p.param_doc -%}
     {% if loop.index == 1 -%}
     @param {{p.last}} {{v}}
     {% else -%}
     * {{v|indent(l,True)}}
     {% endif -%} {% endfor -%}
     {% endif -%}
     */
    public void set{{p.title}}({% if p.mandatory %}@NonNull {% endif %}{{p.return_type}} {{p.last}}) {
        setValue({{p.key}}, {{p.last}});
    }

     /**
     * Gets the {{p.origin}}.
     *
     {% if p.param_doc is not defined -%}
     * @return {{p.return_type}}
    {% else -%}
    {% set l = p.last|length + 8 -%}
     * {% for v in p.param_doc -%}
    {% if loop.index == 1 -%}
     @return {{p.return_type}} {{v}}
     {% else -%}
      * {{v|indent(l,True)}}
     {% endif -%} {% endfor -%}
     {% endif -%}
     */
    {%- if p.SuppressWarnings is defined %}
    @SuppressWarnings("{{p.SuppressWarnings}}")
    {%- endif %}
    public {{p.return_type}} get{{p.title}}() {
        {%- if p.return_type in ['String', 'Boolean', 'Integer'] %}
        return get{{p.return_type}}({{p.key}});
        {%- elif p.return_type in ['Float'] %}
        Object object = getValue({{p.key}});
        return SdlDataTypeConverter.objectToFloat(object);
        {%- else %}
        {%- set clazz = p.return_type %}
        {%- if p.return_type.startswith('List')%}{%set clazz = p.return_type[5:-1]%}{% endif %}
        return ({{p.return_type}}) getObject({{clazz}}.class, {{p.key}});
        {%- endif %}
    }

    {%- endfor %}
    {% endblock -%}
