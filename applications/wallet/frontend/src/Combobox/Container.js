import PropTypes from 'prop-types'
import { Combobox as ReachCombobox } from '@reach/combobox'

import { spacing } from '../Styles'

const ComboboxContainer = ({ onSelect, children }) => {
  return (
    <ReachCombobox
      openOnFocus
      onSelect={onSelect}
      css={{
        position: 'relative',
        paddingTop: spacing.base,
        paddingBottom: spacing.base,
      }}
    >
      {children}
    </ReachCombobox>
  )
}

ComboboxContainer.propTypes = {
  onSelect: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
}

export default ComboboxContainer
