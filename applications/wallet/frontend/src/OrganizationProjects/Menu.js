import { useState } from 'react'
import PropTypes from 'prop-types'
import Link from 'next/link'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'
import Modal from '../Modal'

const OrganizationProjectsMenu = ({ projectId, revalidate }) => {
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)

  return (
    <>
      <Menu open="bottom-left" button={ButtonActions}>
        {({ onBlur, onClick }) => (
          <div>
            <ul>
              <li>
                <Link href={`/${projectId}`} passHref>
                  <Button
                    variant={VARIANTS.MENU_ITEM}
                    onBlur={onBlur}
                    onClick={onClick}
                  >
                    View Project
                  </Button>
                </Link>
              </li>
              <li>
                <Link href={`/${projectId}/users`} passHref>
                  <Button
                    variant={VARIANTS.MENU_ITEM}
                    onBlur={onBlur}
                    onClick={onClick}
                  >
                    Manage Users
                  </Button>
                </Link>
              </li>
              <li>
                <Link href={`/${projectId}/api-keys`} passHref>
                  <Button
                    variant={VARIANTS.MENU_ITEM}
                    onBlur={onBlur}
                    onClick={onClick}
                  >
                    Manage API Keys
                  </Button>
                </Link>
              </li>
              <li>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={() => {
                    onClick()

                    setDeleteModalOpen(true)
                  }}
                >
                  Delete Project
                </Button>
              </li>
            </ul>
          </div>
        )}
      </Menu>
      {isDeleteModalOpen && (
        <Modal
          title="Delete Project"
          message="Deleting this project will remove it and all its content from the system. Deletion will be permanent and irreversible after 30 days."
          action="Delete Permanently"
          onCancel={() => {
            setDeleteModalOpen(false)
          }}
          onConfirm={async () => {
            setDeleteModalOpen(false)

            await fetcher(`/api/v1/projects/${projectId}/`, {
              method: 'DELETE',
            })

            revalidate()
          }}
        />
      )}
    </>
  )
}

OrganizationProjectsMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default OrganizationProjectsMenu
