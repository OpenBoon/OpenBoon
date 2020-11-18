import React from 'react'
import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import Link from 'next/link'

import { spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'

const PaginationNext = ({ currentPage, totalPages }) => {
  const { pathname, query } = useRouter()

  if (currentPage === totalPages) return null

  const queryParam = `?page=${currentPage + 1}`
  const href = `${pathname}${queryParam}`
  const as = href
    .split('/')
    .map((s) => s.replace(/\[(.*)\]/gi, (_, group) => query[group]))
    .join('/')

  return (
    <Link href={href} as={as} passHref>
      <Button
        variant={VARIANTS.SECONDARY_SMALL}
        rel="next"
        style={{
          paddingLeft: spacing.colossal,
          paddingRight: spacing.colossal,
        }}
      >
        Next
      </Button>
    </Link>
  )
}

PaginationNext.propTypes = {
  currentPage: PropTypes.number.isRequired,
  totalPages: PropTypes.number.isRequired,
}

export default PaginationNext
