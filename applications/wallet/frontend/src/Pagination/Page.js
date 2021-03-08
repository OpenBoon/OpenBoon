import { useState } from 'react'
import PropTypes from 'prop-types'
import Router, { useRouter } from 'next/router'

import { spacing, colors, constants } from '../Styles'

import { getQueryString } from '../Fetch/helpers'

const PaginationPage = ({ currentPage, totalPages }) => {
  const { pathname, query } = useRouter()

  const [page, setPage] = useState(currentPage.toString())

  const queryParam = getQueryString({
    query: query.query,
    sort: query.sort,
    filter: query.filter,
    page: page === '1' ? '' : page,
  })
  const href = `${pathname}${queryParam}`
  const as = href
    .split('/')
    .map((s) => s.replace(/\[(.*)\]/gi, (_, group) => query[group]))
    .join('/')

  return (
    <form
      action=""
      method="get"
      onSubmit={(event) => {
        event.preventDefault()
        Router.push(href, as)
      }}
      css={{ display: 'flex' }}
    >
      <input
        key={currentPage}
        aria-label="Go to page"
        type="number"
        min="1"
        max={totalPages}
        css={{
          padding: spacing.base,
          marginLeft: spacing.base,
          textAlign: 'center',
          width: `${Math.max(2.5, page.toString().length)}ch`,
          boxSizing: 'content-box',
          borderRadius: constants.borderRadius.small,
          color: colors.structure.white,
          backgroundColor: colors.structure.smoke,
          border: constants.borders.medium.transparent,
          MozAppearance: 'textfield',
          '&:hover': {
            border: constants.borders.medium.white,
          },
          '&:focus': {
            border: constants.borders.keyOneMedium,
            outline: colors.key.one,
            color: colors.structure.black,
            backgroundColor: colors.structure.white,
          },
          '&::-webkit-outer-spin-button,&::-webkit-inner-spin-button': {
            WebkitAppearance: 'none',
            margin: 0,
          },
        }}
        value={page}
        onFocus={(event) => {
          event.target.select()
        }}
        onChange={({ target: { value } }) => {
          setPage(value)
        }}
        onBlur={() => {
          setPage(currentPage)
        }}
      />
    </form>
  )
}

PaginationPage.propTypes = {
  currentPage: PropTypes.number.isRequired,
  totalPages: PropTypes.number.isRequired,
}

export default PaginationPage
