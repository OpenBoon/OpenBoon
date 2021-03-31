/* eslint-disable react/jsx-props-no-spreading */
import PropTypes from 'prop-types'
import Router, { useRouter } from 'next/router'

import { constants, spacing } from '../Styles'

import { getQueryString } from '../Fetch/helpers'

import InputSearch, { VARIANTS as INPUT_SEARCH_VARIANTS } from '../Input/Search'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import TableContent from './Content'

const Table = ({ role, searchLabel, ...props }) => {
  const {
    pathname,
    query,
    query: { search = '' },
  } = useRouter()

  return (
    <div css={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {!!searchLabel && (
        <div
          css={{
            display: 'flex',
            alignItems: 'flex-end',
            justifyContent: 'space-between',
            paddingBottom: spacing.normal,
            flexShrink: 0,
          }}
        >
          <div
            css={{
              width: constants.form.maxWidth,
              paddingRight: spacing.spacious,
            }}
          >
            <InputSearch
              aria-label={`Filter ${searchLabel}`}
              placeholder={`Filter ${searchLabel}`}
              value={search}
              onChange={({ value }) => {
                const queryParamFilter = getQueryString({
                  query: query.query,
                  sort: query.sort,
                  search: value,
                })
                const href = `${pathname}${queryParamFilter}`
                const as = href
                  .split('/')
                  .map((s) =>
                    s.replace(/\[(.*)\]/gi, (_, group) => query[group]),
                  )
                  .join('/')
                Router.push(href, as)
              }}
              variant={INPUT_SEARCH_VARIANTS.EXTRADARK}
            />
          </div>
        </div>
      )}
      <SuspenseBoundary role={role}>
        <TableContent {...props} />
      </SuspenseBoundary>
    </div>
  )
}

Table.defaultProps = {
  role: null,
  searchLabel: '',
}

Table.propTypes = {
  role: PropTypes.oneOf(Object.keys(ROLES)),
  searchLabel: PropTypes.string,
}

export { Table as default, ROLES }
