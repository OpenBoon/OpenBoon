import PropTypes from 'prop-types'
import { useTable, useRowState, usePagination } from 'react-table'
import { css } from '@emotion/core'
import { colors, spacing } from '../Styles'
import { getPageDescription } from './helpers'

const STATIC_COLUMN_WIDTHS = {
  status: '108px',
  progress: '200px',
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

  const thCSS = css`
     {
      height: ${spacing.spacious}px;
      font-weight: 200;
      color: ${colors.grey2};
      background-color: ${colors.grey1};
      padding: ${spacing.moderate}px ${spacing.normal}px;
      border-bottom: 1px solid ${colors.grey5};
      :not(:last-child) {
        border-right: 1px solid ${colors.grey5};
      }
    }
  `

  const tdCSS = isHovered => {
    return css`
       {
        height: ${spacing.spacious}px;
        font-weight: 200;
        color: ${colors.grey2};
        padding: ${spacing.base}px ${spacing.normal}px;
        background-color: ${isHovered && colors.grey1};
        border: 1px solid transparent;

        :first-of-type {
          border-top: ${isHovered && `1px solid ${colors.grey5}`};
          border-left: ${isHovered && `1px solid ${colors.grey5}`};
          border-bottom: ${isHovered && `1px solid ${colors.grey5}`};
        }

        :last-of-type {
          border-top: ${isHovered && `1px solid ${colors.grey5}`};
          border-right: ${isHovered && `1px solid ${colors.grey5}`};
          border-bottom: ${isHovered && `1px solid ${colors.grey5}`};
        }

        :not(:first-of-type),
        :not(:last-of-type) {
          border-top: ${isHovered && `1px solid ${colors.grey5}`};
          border-bottom: ${isHovered && `1px solid ${colors.grey5}`};
        }
      }
    `
  }

  const trCSS = css`
     {
      background-color: ${colors.grey4};
      :nth-of-type(2n) {
        background-color: ${colors.grey3};
      }
    }
  `

  const paginationBoxCSS = {
    width: `${spacing.large}px`,
    height: `${spacing.spacious}px`,
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
          boxShadow: `0 0 ${spacing.base / 2}px ${colors.black}`,
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
                      <div className="Header__title" css={{ display: 'flex' }}>
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
        <div css={{ paddingRight: `${spacing.base}px` }}>
          {getPageDescription(
            pageSize,
            pageIndex,
            page.length,
            data.length,
            canNextPage,
          )}
        </div>
        <div css={{ paddingRight: `${spacing.base}px` }}>{'Page:'}</div>
        <div css={{ display: 'flex', alignItems: 'center' }}>
          <div
            onClick={() => previousPage()}
            disabled={!canPreviousPage}
            css={{ ...paginationBoxCSS, backgroundColor: colors.grey5 }}>
            {'<'}
          </div>
          <div
            css={{ ...paginationBoxCSS, border: `1px solid ${colors.grey5}` }}>
            {pageIndex + 1}
          </div>
          <div
            css={{
              ...paginationBoxCSS,
              color: colors.grey5,
            }}>{`of ${pageOptions.length}`}</div>
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
