import { useState } from 'react'
import { useRouter } from 'next/router'

import { colors, spacing, constants } from '../Styles'

import { dispatch, ACTIONS } from './helpers'

const BUTTON_SIZE = 42

const Filters = () => {
  const {
    query: { projectId, id: assetId, filters: f = '[]' },
  } = useRouter()
  const filters = JSON.parse(f)

  const [searchString, setSearchString] = useState('')

  const hasSearch = searchString !== ''

  return (
    <form
      action=""
      method="post"
      onSubmit={(event) => event.preventDefault()}
      css={{
        flex: 1,
        backgroundColor: colors.structure.lead,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        padding: spacing.small,
      }}
    >
      <div css={{ display: 'flex' }}>
        <input
          type="text"
          placeholder="Create text filter (search name or field value)"
          value={searchString}
          onChange={({ target: { value } }) => setSearchString(value)}
          css={{
            flex: 1,
            border: 'none',
            padding: spacing.moderate,
            borderTopLeftRadius: constants.borderRadius.small,
            borderBottomLeftRadius: constants.borderRadius.small,
            color: colors.structure.pebble,
            backgroundColor: colors.structure.coal,
            '&:focus': {
              color: colors.structure.coal,
              backgroundColor: colors.structure.white,
            },
          }}
        />
        <button
          type="submit"
          onClick={() => {
            setSearchString('')
            dispatch({
              action: ACTIONS.ADD_FILTER,
              payload: {
                projectId,
                assetId,
                filters,
                filter: { search: searchString },
              },
            })
          }}
          css={{
            width: BUTTON_SIZE,
            borderTopRightRadius: constants.borderRadius.small,
            borderBottomRightRadius: constants.borderRadius.small,
            color: hasSearch ? colors.structure.white : colors.structure.black,
            backgroundColor: hasSearch
              ? colors.key.one
              : colors.structure.steel,
            margin: 0,
            padding: 0,
            border: 0,
          }}
        >
          +
        </button>
      </div>
    </form>
  )
}

export default Filters
