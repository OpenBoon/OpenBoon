import PropTypes from 'prop-types'
import JSONPretty from 'react-json-pretty'

import { colors } from '../Styles'

const JsonDisplay = ({ json }) => (
  <JSONPretty
    id="json-pretty"
    data={json}
    theme={{
      main: `margin:0;line-height:1.3;overflow:auto;`,
      string: `color:${colors.signal.grass.base};`,
      value: `color:${colors.signal.sky.base};`,
      boolean: `color:${colors.signal.canary.base};`,
    }}
  />
)

JsonDisplay.propTypes = {
  json: PropTypes.shape({}).isRequired,
}

export default JsonDisplay
