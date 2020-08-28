import PropTypes from 'prop-types'

import { spacing } from '../Styles'

import InputSearch, { VARIANTS as INPUT_SEARCH_VARIANTS } from '../Input/Search'

const FilterSearch = ({ placeholder, searchString, onChange }) => {
  return (
    <div css={{ paddingBottom: spacing.normal }}>
      <InputSearch
        aria-label={placeholder}
        placeholder={placeholder}
        value={searchString}
        onChange={onChange}
        variant={INPUT_SEARCH_VARIANTS.DARK}
      />
    </div>
  )
}

FilterSearch.propTypes = {
  placeholder: PropTypes.string.isRequired,
  searchString: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
}

export default FilterSearch
