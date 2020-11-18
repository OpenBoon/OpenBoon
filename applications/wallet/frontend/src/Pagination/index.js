import PropTypes from 'prop-types'

import { spacing, colors } from '../Styles'

import PaginationNext from './Next'
import PaginationLink from './Link'
import PaginationJump from './Jump'
import PaginationPage from './Page'

const Pagination = ({ currentPage, totalPages }) => {
  if (totalPages === 1) return null

  return (
    <div css={{ display: 'flex', flexShrink: 0 }}>
      <div css={{ flex: 1 }} />

      <div css={{ flex: 1, display: 'flex', justifyContent: 'center' }}>
        <PaginationNext currentPage={currentPage} totalPages={totalPages} />
      </div>

      <div
        css={{
          flex: 1,
          display: 'flex',
          justifyContent: 'flex-end',
          flexShrink: 0,
        }}
      >
        <PaginationJump
          currentPage={currentPage}
          totalPages={totalPages}
          direction="prev"
        />

        <PaginationLink
          currentPage={currentPage}
          totalPages={totalPages}
          direction="prev"
        />

        <PaginationPage
          key={currentPage}
          currentPage={currentPage}
          totalPages={totalPages}
        />

        <div
          css={{
            display: 'flex',
            alignItems: 'center',
            paddingLeft: spacing.base,
            paddingRight: spacing.base,
            color: colors.structure.zinc,
          }}
        >
          of {totalPages}
        </div>

        <PaginationLink
          currentPage={currentPage}
          totalPages={totalPages}
          direction="next"
        />

        <PaginationJump
          currentPage={currentPage}
          totalPages={totalPages}
          direction="next"
        />
      </div>
    </div>
  )
}

Pagination.propTypes = {
  currentPage: PropTypes.number.isRequired,
  totalPages: PropTypes.number.isRequired,
}

export default Pagination
