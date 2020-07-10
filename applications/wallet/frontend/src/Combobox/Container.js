import PropTypes from 'prop-types'
import { Combobox as ReachCombobox } from '@reach/combobox'

const ComboboxContainer = ({ onSelect, children }) => {
  return (
    <ReachCombobox openOnFocus onSelect={onSelect}>
      {children}
    </ReachCombobox>
  )
}

ComboboxContainer.propTypes = {
  onSelect: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
}

export default ComboboxContainer
