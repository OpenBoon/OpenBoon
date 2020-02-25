import PropTypes from 'prop-types'

import { spacing } from '../Styles'

const MessageDisplay = ({ title, content }) => {
  return (
    <>
      <div
        css={{
          fontWeight: 700,
          paddingRight: spacing.base,
          whiteSpace: 'noWrap',
          paddingBottom: spacing.small,
        }}>
        {`${title}:`}
      </div>
      {content}
    </>
  )
}

MessageDisplay.propTypes = {
  title: PropTypes.string.isRequired,
  content: PropTypes.string.isRequired,
}

export default MessageDisplay
