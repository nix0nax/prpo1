package Serv;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import org.eclipse.jetty.util.log.Log;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.sql.*;
import java.io.*;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;


@WebServlet("/servlet")
public class FirstServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        PrintWriter out = resp.getWriter();
        out.println("bruh momento numero 6 jajajajjajajajajaajaj\nkdo za vraga je jansa");

        String verzija = ConfigurationUtil.getInstance().get("kumuluzee.version").get();
        String imekum = ConfigurationUtil.getInstance().get("kumuluzee.name").get();

        out.println("ime: " + imekum + "\nverzija:  " + verzija);

        UporabnikDaoImpl updao = new UporabnikDaoImpl();
        updao.vstavi(new Uporabnik("Joze", "Gorisek", "delam-v-tamu-ja"));
        updao.vstavi(new Uporabnik("Nenez", "Nensa", "kdo_za_vraga_je_janez"));
        out.println("Uporabnik z id 1 je " + updao.vrni(1));
        out.println("Tabela uporabnikov:\n" + updao.vrniVse());
        updao.posodobi(new Uporabnik( 1, "Joze", "Gorisek", "delam-na-fus"));
        updao.odstrani(2);
        out.println("Tabela po posodobitvi in izbrisu:\n" + updao.vrniVse());

    }
}

abstract class Entiteta implements Serializable {
    int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}

class Uporabnik extends Entiteta {
    int id;
    String ime, priimek, username;


    public Uporabnik(String ime, String priimek, String username) {
        this.ime = ime;
        this.priimek = priimek;
        this.username = username;
    }

    public Uporabnik(int id, String ime, String priimek, String username) {
        this.id = id;
        this.ime = ime;
        this.priimek = priimek;
        this.username = username;
    }

    public String getIme() {
        return ime;
    }

    public void setIme(String ime) {
        this.ime = ime;
    }

    public String getPriimek() {
        return priimek;
    }

    public void setPriimek(String priimek) {
        this.priimek = priimek;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Uporabnik{" +
                "id=" + id +
                ", ime='" + ime + '\'' +
                ", priimek='" + priimek + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}

interface BaseDao {
    Connection getConnection() throws NamingException, SQLException ;

    Entiteta vrni(int id);

    void vstavi(Entiteta ent);

    void odstrani(int id);

    void posodobi(Entiteta ent);

    List<Entiteta> vrniVse();
}

class UporabnikDaoImpl implements BaseDao {

    Connection con;
    private static final Logger log = Logger.getLogger(FirstServlet.class.getName());

    @Override
    public Connection getConnection() throws NamingException, SQLException {
        InitialContext initCtx = new InitialContext();
        DataSource ds = (DataSource) initCtx.lookup("jdbc/SimpleJdbcDS");
        return ds.getConnection();
    }

    @Override
    public Entiteta vrni(int id) {

        PreparedStatement ps = null;

        try {

            if (con == null) {
                con = getConnection();
            }

            String sql = "SELECT * FROM uporabnik WHERE id = ?";
            ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Uporabnik ret = getUporabnikFromRS(rs);
                ret.setId(id);
                return ret;
            } else {
                log.info("Uporabnik ne obstaja");
            }

        } catch (SQLException | NamingException e) {
            log.severe(e.toString());
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    log.severe(e.toString());
                }
            }
        }
        return null;
    }

    @Override
    public List<Entiteta> vrniVse() {

        PreparedStatement ps = null;
        LinkedList<Entiteta> ret = new LinkedList<Entiteta>();

        try {

            if (con == null) {
                con = getConnection();
            }

            String sql = "SELECT * FROM uporabnik";
            ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Uporabnik toadd = getUporabnikFromRS(rs);
                toadd.setId(rs.getInt("id"));
                log.info(toadd.toString());
                ret.add(toadd);
            }
            return ret;

        } catch (SQLException | NamingException e) {
            log.severe(e.toString());
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    log.severe(e.toString());
                }
            }
        }
        return null;
    }

    @Override
    public void vstavi(Entiteta toadd) {

        PreparedStatement ps = null;

        try {

            if (con == null) {
                con = getConnection();
            }

            String sql = "INSERT INTO  uporabnik(ime, priimek, username) VALUES (?, ?, ?)";
            ps = con.prepareStatement(sql,
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, ((Uporabnik) toadd).getIme());
            ps.setString(2, ((Uporabnik) toadd).getPriimek());
            ps.setString(3, ((Uporabnik) toadd).getUsername());
            ps.executeUpdate();
            ResultSet genkey = ps.getGeneratedKeys();
            genkey.next();
            toadd.setId(genkey.getInt(1));

        } catch (SQLException | NamingException e) {
            log.severe(e.toString());
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    log.severe(e.toString());
                }
            }
        }
    }

    @Override
    public void odstrani(int id) {

        PreparedStatement ps = null;

        try {

            if (con == null) {
                con = getConnection();
            }

            String sql = "DELETE FROM  uporabnik WHERE id = ?";
            ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeQuery();

        } catch (SQLException | NamingException e) {
            log.severe(e.toString());
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    log.severe(e.toString());
                }
            }
        }
    }

    @Override
    public void posodobi(Entiteta toupdate) {

        PreparedStatement ps = null;

        try {

            if (con == null) {
                con = getConnection();
            }

            String sql = "UPDATE uporabnik SET ime = ?, priimek = ?, username = ? WHERE id = ?";
            ps = con.prepareStatement(sql);
            ps.setInt(4, toupdate.getId());
            ps.setString(1, ((Uporabnik) toupdate).getIme());
            ps.setString(2, ((Uporabnik) toupdate).getPriimek());
            ps.setString(3, ((Uporabnik) toupdate).getUsername());
            ps.executeQuery();

        } catch (SQLException | NamingException e) {
            log.severe(e.toString());
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    log.severe(e.toString());
                }
            }
        }
    }

    private Uporabnik getUporabnikFromRS(ResultSet rs) throws SQLException {
        String ime = rs.getString("ime");
        String priimek = rs.getString("priimek");
        String username = rs.getString("username");
        return new Uporabnik(ime, priimek, username);
    }
}