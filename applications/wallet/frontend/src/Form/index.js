import PropTypes from 'prop-types'

import { spacing, constants } from '../Styles'

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
          width: constants.form.maxWidth,
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
