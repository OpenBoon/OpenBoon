import PropTypes from 'prop-types'

import { spacing } from '../Styles'

export const MAX_WIDTH = 470

const Form = ({ children, style }) => {
  return (
    <form
      action=""
      method="post"
      onSubmit={event => event.preventDefault()}
      css={[
        {
          display: 'flex',
          flexDirection: 'column',
          width: MAX_WIDTH,
          padding: spacing.normal,
          paddingLeft: 0,
          paddingRight: 0,
        },
        style,
      ]}>
      {children}
    </form>
  )
}

Form.defaultProps = {
  style: {},
}

Form.propTypes = {
  children: PropTypes.node.isRequired,
  style: PropTypes.shape({ name: PropTypes.string, styles: PropTypes.string }),
}

export default Form
