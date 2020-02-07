import PropTypes from 'prop-types'
import { spacing } from '../Styles'

const ButtonGroup = ({ justify, children }) => {
  return (
    <div
      css={{
        display: 'flex',
        justifyContent: `${justify}`,
        paddingTop: spacing.normal,
        'button, [type="button"]': {
          '&:not(:first-of-type)': {
            marginRight: spacing.normal,
          },
        },
      }}>
      {children}
    </div>
  )
}

ButtonGroup.defaultProps = {
  justify: 'left',
}

ButtonGroup.propTypes = {
  justify: PropTypes.string,
  children: PropTypes.node.isRequired,
}

export default ButtonGroup
