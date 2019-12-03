import PropTypes from 'prop-types'

const FormattedColumn = ({ style, content }) => {
  return <div style={style}>{content}</div>
}

FormattedColumn.propTypes = {
  style: PropTypes.object.isRequired,
  content: PropTypes.string.isRequired,
}

export default FormattedColumn
