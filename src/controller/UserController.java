package controller;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import action.ActionAnnotation;
import action.RequestMapping;
import action.RequestMapping.RequestMethod;
import model.User;
import repository.MybatisUserDao;
import util.Gmail;
import util.KakaoAPI;
import util.SHA256;

@SuppressWarnings("serial")
public class UserController extends ActionAnnotation {

	@Override
	public void initProcess(HttpServletRequest request, HttpServletResponse response) {

	}

	@RequestMapping(value = "loginForm", method = RequestMethod.GET)
	public String loginForm(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String code = request.getParameter("code");
//		System.out.println("code: " + code);
		KakaoAPI kakao = new KakaoAPI();
		String access_Token = kakao.getAccessToken(code);
//      System.out.println("controller access_token : " + access_Token);
		HashMap<String, Object> userInfo = kakao.getUserInfo(access_Token);
		System.out.println("login Controller : " + userInfo);

		if (userInfo.get("email") != null) {
			HttpSession session = request.getSession();
			session.setAttribute("userId", userInfo.get("email"));
			session.setAttribute("access_Token", access_Token);
		}

		return "/WEB-INF/view/user/loginForm.jsp";
	}

	@RequestMapping(value = "loginPro", method = RequestMethod.POST)
	public String loginPro(HttpServletRequest request, HttpServletResponse response) throws Exception {

		return "/WEB-INF/view/member/loginPro.jsp";
	}

	@RequestMapping(value = "logoutForm", method = RequestMethod.GET)
	public String logout(HttpSession session) {
		KakaoAPI kakao = new KakaoAPI();
		kakao.kakaoLogout((String) session.getAttribute("access_Token"));
		session.removeAttribute("access_Token");
		session.removeAttribute("userId");
		return "/WEB-INF/view/user/loginForm.jsp";
	}

	// ȸ������ ��
	@RequestMapping(value = "joinForm", method = RequestMethod.GET)
	public String joinForm(HttpServletRequest request, HttpServletResponse response) throws Exception {

		return "/WEB-INF/view/user/joinForm.jsp";
	}

	// ȸ������ ó�� (�̸��� ����)
	@RequestMapping(value = "joinPro", method = RequestMethod.POST)
	public String joinPro(HttpServletRequest request, HttpServletResponse response) throws Exception {

		request.setCharacterEncoding("utf-8");

		HttpSession session = request.getSession();
		String userId = request.getParameter("id");
		String userPasswd = request.getParameter("password");
		String userName = request.getParameter("name");
		String userEmail = request.getParameter("email");
		String userEmailHash = SHA256.getSHA256(userEmail);
		int userEmailCheck = 0;
		String userPhone = request.getParameter("phone1") + request.getParameter("phone2")
				+ request.getParameter("phone3");
		String userAddress = request.getParameter("address");

		MybatisUserDao service = MybatisUserDao.getInstance();

		User user = new User();

		user.setUserId(userId);
		user.setUserPassword(userPasswd);
		user.setUserName(userName);
		user.setUserEmail(userEmail);
		user.setUserEmailHash(userEmailHash);
		user.setUserEmailCheck(userEmailCheck);
		user.setUserPhone(userPhone);
		user.setUserAddress(userAddress);

		service.joinUser(user);
		session.setAttribute("userId", userId);

		return "redirect:/user/joinSendEmail";
	}

	// �������� ������
	@RequestMapping(value = "joinSendEmail", method = RequestMethod.GET)
	public String joinSendEmail(HttpServletRequest request, HttpServletResponse response) throws Exception {

		MybatisUserDao service = MybatisUserDao.getInstance();
		HttpSession session = request.getSession();

		String userId = null;

		// ���ǿ� ����� id�� null�� �ƴ϶�� �� ����
		if (session.getAttribute("userId") != null) {
			userId = (String) session.getAttribute("userId");
		}

		if (userId == null) {
			// userId�� ���ٸ� �α��������� �̵�
			PrintWriter script = response.getWriter();
			script.println("<script>");
			script.println("alert('�α����� ���ּ���.');");
			script.println("</script>");
			script.close();

			return "redirect:/user/loginForm";
		}

		int emailChecked = service.getUserEmailChecked(userId);
		System.out.println(emailChecked);
		if (emailChecked == 1) {

			PrintWriter script = response.getWriter();
			script.println("<script>");
			script.println("alert('�̹� ���� �� ȸ���Դϴ�.');");
			script.println("</script>");
			script.close();

			return "redirect:/main/main";
		}else if (emailChecked == 0) {
			// ����ڿ��� ���� �޽����� �����մϴ�.
			String host = "http://localhost:8080/zSpringProject/";
			String from = "oakNutSpring@gmail.com";
			String to = service.getUserEmail(userId);

			String subject = "���丮���� ȸ������ �̸��� ����!";

			String content = "���� ��ũ�� �����Ͽ� �̸��� Ȯ���� �������ּ���." +

					"<a href='" + host + "joinEmailCheckPro?code=" + new SHA256().getSHA256(to) + "'>�̸��� �����ϱ�</a>";

			// SMTP�� �����ϱ� ���� ������ �����մϴ�.

			Properties p = new Properties();
			p.put("mail.smtp.user", from);
			p.put("mail.smtp.host", "smtp.googlemail.com");
			p.put("mail.smtp.port", "465");
			p.put("mail.smtp.starttls.enable", "true");
			p.put("mail.smtp.auth", "true");
			p.put("mail.smtp.debug", "true");
			p.put("mail.smtp.socketFactory.port", "465");
			p.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			p.put("mail.smtp.socketFactory.fallback", "false");

			try {

				Authenticator auth = new Gmail();

				Session ses = Session.getInstance(p, auth);

				ses.setDebug(true);

				MimeMessage msg = new MimeMessage(ses);

				msg.setSubject(subject);

				Address fromAddr = new InternetAddress(from);

				msg.setFrom(fromAddr);

				Address toAddr = new InternetAddress(to);

				msg.addRecipient(Message.RecipientType.TO, toAddr);

				msg.setContent(content, "text/html;charset=UTF-8");

				Transport.send(msg);

			} catch (Exception e) {

				e.printStackTrace();

				PrintWriter script = response.getWriter();

				script.println("<script>");

				script.println("alert('������ �߻��߽��ϴ�..');");

				script.println("</script>");

				script.close();

				return "redirect:/user/joinForm";

			}
			return "/WEB-INF/view/user/joinSendEmail.jsp";
		}
		return "/WEB-INF/view/user/joinSendEmail.jsp";
	}
}