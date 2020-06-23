import PropTypes from 'prop-types'

import { capitalizeFirstLetter } from '../Metadata/helpers'

const ChartFormOptions = ({ fields, type }) => {
  return Object.keys(fields).map((option) => {
    if (Array.isArray(fields[option])) {
      return <option>{option}</option>
    }

    return (
      <optgroup key={option} label={capitalizeFirstLetter({ word: option })}>
        <ChartFormOptions fields={fields[option]} type={type} />
      </optgroup>
    )
  })
}

ChartFormOptions.propTypes = {
  fields: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.string),
    PropTypes.shape({}),
  ]).isRequired,
  type: PropTypes.oneOf(['FACET', 'RANGE']).isRequired,
}

export default ChartFormOptions
