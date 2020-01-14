import PropTypes from 'prop-types'

import { spacing } from '../Styles'

const MAX_WIDTH = 470

const Form = ({ children }) => {
  return (
    <form
      action=""
      method="post"
      onSubmit={event => event.preventDefault()}
      css={{
        display: 'flex',
        flexDirection: 'column',
        width: MAX_WIDTH,
        padding: spacing.normal,
        paddingLeft: 0,
        paddingRight: 0,
      }}>
      {children}
    </form>
  )
}

Form.propTypes = {
  children: PropTypes.node.isRequired,
}

export default Form
