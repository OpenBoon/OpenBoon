import PropTypes from 'prop-types'
import JSONPretty from 'react-json-pretty'

import { colors, spacing } from '../Styles'

const JsonDisplay = ({ json }) => (
  <div
    css={{
      backgroundColor: colors.structure.coal,
      pre: { padding: spacing.normal },
    }}
  >
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
  </div>
)

JsonDisplay.propTypes = {
  json: PropTypes.oneOfType([PropTypes.object, PropTypes.array]).isRequired,
}

export default JsonDisplay
