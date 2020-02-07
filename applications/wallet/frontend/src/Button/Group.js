import PropTypes from 'prop-types'
import { spacing } from '../Styles'

const ButtonGroup = ({ children }) => {
  return (
    <div
      css={{
        display: 'flex',
        paddingTop: spacing.normal,
        button: {
          marginRight: spacing.normal,
        },
      }}>
      {children}
    </div>
  )
}

ButtonGroup.propTypes = {
  children: PropTypes.node.isRequired,
}

export default ButtonGroup
