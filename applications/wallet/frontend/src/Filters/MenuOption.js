import PropTypes from 'prop-types'

import { spacing, colors, constants } from '../Styles'

import Checkbox, { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'

const OFFSET = 32

const FiltersMenuOption = ({ option }) => {
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
            label: option,
            initialValue: false,
            isDisabled: false,
          }}
          onClick={console.warn}
        />
      </div>
    </div>
  )
}

FiltersMenuOption.propTypes = {
  option: PropTypes.string.isRequired,
}

export default FiltersMenuOption
