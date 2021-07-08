import PropTypes from 'prop-types'
import { spacing } from '../Styles'

const ButtonGroup = ({ children }) => {
  return (
    <div
      css={{
        display: 'flex',
        paddingTop: spacing.normal,
        paddingBottom: spacing.normal,
        'button, a': {
          marginRight: spacing.normal,
        },
        'button:last-child, a:last-child': {
          marginRight: 0,
        },
      }}
    >
      {children}
    </div>
  )
}

ButtonGroup.propTypes = {
  children: PropTypes.node.isRequired,
}

export default ButtonGroup
