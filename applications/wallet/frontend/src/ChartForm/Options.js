import PropTypes from 'prop-types'

import chartShape from '../Chart/shape'

import { capitalizeFirstLetter } from '../Metadata/helpers'

const ChartFormOptions = ({ fields, type, path }) => {
  return Object.keys(fields).map((option) => {
    if (Array.isArray(fields[option])) {
      const fullPath = `${path}.${option}`

      return (
        <option key={fullPath} value={fullPath}>
          {option}
        </option>
      )
    }

    return (
      <optgroup key={option} label={capitalizeFirstLetter({ word: option })}>
        <ChartFormOptions
          fields={fields[option]}
          type={type}
          path={path ? `${path}.${option}` : option}
        />
      </optgroup>
    )
  })
}

ChartFormOptions.propTypes = {
  fields: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.string),
    PropTypes.shape({}),
  ]).isRequired,
  type: chartShape.type.isRequired,
  path: PropTypes.string.isRequired,
}

export default ChartFormOptions
