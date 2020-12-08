import PropTypes from 'prop-types'

import stylesShape from '../Styles/shape'

import { colors, spacing } from '../Styles'

import SelectContent from './Content'

const Select = ({
  label,
  options,
  defaultValue,
  onChange,
  isRequired,
  isDisabled,
  useAria,
  style,
}) => {
  if (useAria) {
    return (
      <SelectContent
        label={label}
        options={options}
        defaultValue={defaultValue}
        onChange={onChange}
        isDisabled={isDisabled}
        useAria={useAria}
        style={style}
      />
    )
  }

  return (
    <label css={{ color: colors.structure.zinc }}>
      {label}
      {isRequired && (
        <span css={{ color: colors.signal.warning.base }}> *</span>
      )}
      <div css={{ paddingTop: spacing.base, paddingBottom: spacing.base }}>
        <SelectContent
          label={label}
          options={options}
          defaultValue={defaultValue}
          onChange={onChange}
          isDisabled={isDisabled}
          useAria={useAria}
          style={style}
        />
      </div>
    </label>
  )
}

Select.defaultProps = {
  useAria: false,
  defaultValue: '',
  isDisabled: false,
  style: {},
}

Select.propTypes = {
  label: PropTypes.node.isRequired,
  options: PropTypes.arrayOf(
    PropTypes.shape({
      value: PropTypes.string,
      label: PropTypes.string,
    }),
  ).isRequired,
  defaultValue: PropTypes.string,
  onChange: PropTypes.func.isRequired,
  isRequired: PropTypes.bool.isRequired,
  isDisabled: PropTypes.bool,
  useAria: PropTypes.bool,
  style: stylesShape,
}

export default Select
