import PropTypes from 'prop-types'

import filterShape from '../Filter/shape'

import { spacing, colors, constants } from '../Styles'

import Checkbox, { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'

const OFFSET = 32

const FiltersMenuOption = ({ option, label, filters, onClick }) => {
  const isEnabled = filters.find(({ attribute }) => attribute === option)

  return (
    <div
      key={option}
      css={{
        marginLeft: OFFSET,
        borderTop: constants.borders.divider,
        ':first-of-type': {
          borderTop: 'none',
        },
      }}
    >
      <div
        css={{
          padding: spacing.base,
          color: colors.structure.zinc,
          marginLeft: -OFFSET,
        }}
      >
        <Checkbox
          key={option}
          variant={CHECKBOX_VARIANTS.SMALL}
          option={{
            value: option,
            label,
            initialValue: !!isEnabled,
            isDisabled: !!isEnabled,
          }}
          onClick={onClick}
        />
      </div>
    </div>
  )
}

FiltersMenuOption.propTypes = {
  option: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  onClick: PropTypes.func.isRequired,
}

export default FiltersMenuOption
