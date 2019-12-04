import PropTypes from 'prop-types'
import { useTable, useRowState, usePagination } from 'react-table'
import { colors, spacing, typography } from '../Styles'
import { getPageDescription } from './helpers'

const STATIC_COLUMN_WIDTHS = {
  status: '108px',
  progress: '200px',
}
const ROW_HEIGHT = 32
const PAGINATION_HEIGHT = 32
const PAGINATION_WIDTH = 40

const tdCSS = isHovered => {
  return {
    height: ROW_HEIGHT,
    fontWeight: typography.weight.extraLight,
    color: colors.grey2,
    padding: `${spacing.base}px ${spacing.normal}px`,
    backgroundColor: `${isHovered && colors.grey1}`,
    border: `1px solid transparent`,

    '&:first-of-type': {
      borderTop: `${isHovered && `1px solid ${colors.grey5}`}`,
      borderLeft: `${isHovered && `1px solid ${colors.grey5}`}`,
      borderBottom: `${isHovered && `1px solid ${colors.grey5}`}`,
    },

    '&:last-of-type': {
      borderTop: `${isHovered && `1px solid ${colors.grey5}`}`,
      borderRight: `${isHovered && `1px solid ${colors.grey5}`}`,
      borderBottom: `${isHovered && `1px solid ${colors.grey5}`}`,
    },

    '&:not(:first-of-type), &:not(:last-of-type)': {
      borderTop: `${isHovered && `1px solid ${colors.grey5}`}`,
      borderBottom: `${isHovered && `1px solid ${colors.grey5}`}`,
    },
  }
}

const Table = ({ columns, data }) => {
  const {
    getTableProps,
    getTableBodyProps,
    headerGroups,
    prepareRow,

    pageOptions,
    page,
    state: { pageIndex, pageSize },
    previousPage,
    nextPage,
    canPreviousPage,
    canNextPage,
  } = useTable(
    { columns, data, initialState: { pageSize: 5 } },
    useRowState,
    usePagination,
  )

  const thCSS = {
    height: ROW_HEIGHT,
    fontWeight: typography.weight.extraLight,
    color: colors.grey2,
    backgroundColor: colors.grey1,
    padding: `${spacing.moderate}px ${spacing.normal}px`,
    borderBottom: `1px solid ${colors.grey5}`,
    '&:not(:last-child)': {
      borderRight: `1px solid ${colors.grey5}`,
    },
  }

  const trCSS = {
    backgroundColor: colors.grey4,
    '&:nth-of-type(2n)': {
      backgroundColor: colors.grey3,
    },
  }

  const paginationBoxCSS = {
    width: PAGINATION_WIDTH,
    height: PAGINATION_HEIGHT,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: colors.grey2,
    cursor: 'pointer',
  }

  return (
    <div css={{ display: 'flex', flexDirection: 'column' }}>
      <table
        {...getTableProps()}
        css={{
          borderSpacing: 0,
          boxShadow: `0 0 ${spacing.base / 2}px ${colors.black} `,
          margin: `${spacing.base}px 0`,
        }}>
        <thead>
          {headerGroups.map(headerGroup => {
            const { key, ...rest } = headerGroup.getHeaderGroupProps()
            return (
              <tr key={key} {...rest}>
                {headerGroup.headers.map(column => {
                  const { key, ...rest } = column.getHeaderProps()
                  return (
                    <th
                      key={key}
                      {...rest}
                      css={thCSS}
                      style={{ width: STATIC_COLUMN_WIDTHS[column.id] }}>
                      <div css={{ display: 'flex' }}>
                        {column.render('Header')}
                      </div>
                    </th>
                  )
                })}
              </tr>
            )
          })}
        </thead>
        <tbody {...getTableBodyProps()}>
          {page.map(row => {
            prepareRow(row)
            const { key, ...rest } = row.getRowProps()
            return (
              <tr
                key={key}
                css={trCSS}
                onMouseEnter={() => {
                  row.setState({ isHovered: true })
                }}
                onMouseLeave={() => {
                  row.setState({ isHovered: false })
                }}
                {...rest}>
                {row.cells.map(cell => {
                  const { key, ...rest } = cell.getCellProps()
                  return (
                    <td
                      key={key}
                      {...rest}
                      css={tdCSS(cell.row.state.isHovered)}>
                      {cell.render('Cell')}
                    </td>
                  )
                })}
              </tr>
            )
          })}
        </tbody>
      </table>
      <div
        css={{
          display: 'flex',
          alignSelf: 'flex-end',
          alignItems: 'center',
          color: colors.grey5,
        }}>
        <div css={{ paddingRight: spacing.base }}>
          {getPageDescription({
            pageSize,
            pageIndex,
            numRowsOnPage: page.length,
            numRowsTotal: data.length,
            canNextPage,
          })}
        </div>
        <div css={{ paddingRight: spacing.base }}>{'Page:'}</div>
        <div css={{ display: 'flex', alignItems: 'center' }}>
          <div
            onClick={() => previousPage()}
            disabled={!canPreviousPage}
            css={{ ...paginationBoxCSS, backgroundColor: colors.grey5 }}>
            {'<'}
          </div>
          <div
            css={{ ...paginationBoxCSS, border: `1px solid ${colors.grey5} ` }}>
            {pageIndex + 1}
          </div>
          <div
            css={{
              ...paginationBoxCSS,
              color: colors.grey5,
            }}>{`of ${pageOptions.length} `}</div>
          <div
            onClick={() => nextPage()}
            disabled={!canNextPage}
            css={{ ...paginationBoxCSS, backgroundColor: colors.grey5 }}>
            {'>'}
          </div>
        </div>
      </div>
    </div>
  )
}

Table.propTypes = {
  columns: PropTypes.array.isRequired,
  data: PropTypes.array.isRequired,
}

export default Table
